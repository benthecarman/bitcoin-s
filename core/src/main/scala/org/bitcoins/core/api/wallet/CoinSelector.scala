package org.bitcoins.core.api.wallet

import org.bitcoins.core.api.wallet.CoinSelectionAlgo._
import org.bitcoins.core.currency._
import org.bitcoins.core.protocol.script._
import org.bitcoins.core.protocol.transaction._
import org.bitcoins.core.wallet.fee.FeeUnit

import scala.annotation.tailrec
import scala.util.{Random, Try}

/** Implements algorithms for selecting from a UTXO set to spend to an output set at a given fee rate. */
trait CoinSelector {

  private val BNB_MAX_TRIES = 100000

  /** Selects coins using the branch and bound algorithm.
    * @see https://github.com/bitcoin/bitcoin/blob/7f79746bf046d0028bb68f265804b9774dec2acb/src/wallet/coinselection.cpp#L65
    */
  def selectCoinsBnB(
      walletUtxos: Vector[CoinSelectorUtxo],
      outputs: Vector[TransactionOutput],
      changeCost: CurrencyUnit,
      feeRate: FeeUnit,
      longTermFeeRate: FeeUnit): Vector[CoinSelectorUtxo] = {
    // target is the total of the outputs we are trying to spend
    // plus the 10 overhead bytes of the transaction and size of the outputs
    val nonInputFees = feeRate * (outputs.map(_.byteSize).sum + 10)
    val target =
      outputs.foldLeft(CurrencyUnits.zero)(_ + _.value) + nonInputFees

    // Filter dust coins
    val usableUtxos =
      walletUtxos.filter(_.effectiveValue(feeRate) > CurrencyUnits.zero)

    val startingValue =
      usableUtxos.foldLeft(CurrencyUnits.zero)(_ + _.effectiveValue(feeRate))

    if (startingValue < target) {
      throw new RuntimeException(
        s"Not enough value in given outputs to make transaction spending $target")
    }

    val sorted = usableUtxos.sortBy(_.value)(Ordering[CurrencyUnit].reverse)

    @tailrec
    def loop(
        currentValue: CurrencyUnit,
        currentAvailValue: CurrencyUnit,
        currentWaste: CurrencyUnit,
        bestWaste: CurrencyUnit,
        currentSelection: Vector[CoinSelectorUtxo],
        bestSelection: Vector[CoinSelectorUtxo],
        tries: Int): Vector[CoinSelectorUtxo] = {
      if (tries > BNB_MAX_TRIES) {
        throw new RuntimeException(
          s"Failed to find a solution after $BNB_MAX_TRIES tries")
      }

      val (backtrack, newBestWaste, newBestSelection) = {
        if (
          currentValue + currentAvailValue < target || // Cannot possibly reach target with the amount remaining in the currentAvailValue.
          currentValue > target + changeCost || // Selected value is out of range, go back and try other branch
          (currentWaste > bestWaste && (sorted.head.fee(feeRate) - sorted.head
            .fee(longTermFeeRate)) > Satoshis.zero)
        ) { // Don't select things which we know will be more wasteful if the waste is increasing
          (true, bestWaste, bestSelection)
        } else if (currentValue >= target) { // Selected value is within range
          // This is the excess value which is added to the waste for the below comparison
          // Adding another UTXO after this check could bring the waste down if the long term fee is higher than the current fee.
          // However we are not going to explore that because this optimization for the waste is only done when we have hit our target
          // value. Adding any more UTXOs will be just burning the UTXO; it will go entirely to fees. Thus we aren't going to
          // explore any more UTXOs to avoid burning money like that.
          if (currentWaste + (currentValue - target) <= bestWaste) {
            (true, currentWaste, currentSelection)
          } else (true, bestWaste, bestSelection)
        } else (false, bestWaste, bestSelection)
      }

      if (backtrack && currentSelection.isEmpty) {
        bestSelection // We have walked back to the first utxo and no branch is untraversed. All solutions searched
      } else if (backtrack && currentSelection.nonEmpty) {} else { // Moving forwards, continuing down this branch
        val utxo = sorted(currentSelection.size)
        val effectiveValue = utxo.effectiveValue(feeRate)

        // Remove this utxo from the currentAvailValue utxo amount
        val newAvailValue = currentAvailValue - effectiveValue

        // Avoid searching a branch if the previous UTXO has the same value and same waste and was excluded. Since the ratio of fee to
        // long term fee is the same, we only need to check if one of those values match in order to know that the waste is the same.
        // todo missing !curr_selection.back()
        if (
          currentSelection.nonEmpty &&
          (effectiveValue == sorted(currentSelection.size - 1).effectiveValue(
            feeRate)) &&
          (utxo.fee(feeRate) == sorted(currentSelection.size - 1).fee(feeRate))
        ) {
          loop(
            currentValue = currentValue,
            currentAvailValue = newAvailValue,
            currentWaste = currentWaste,
            bestWaste = newBestWaste,
            currentSelection = currentSelection,
            bestSelection = newBestSelection,
            tries = tries + 1
          )
        } else { // Inclusion branch first (Largest First Exploration)
          loop(
            currentValue = currentValue + effectiveValue,
            currentAvailValue = newAvailValue,
            currentWaste = utxo.fee(feeRate) - utxo.fee(longTermFeeRate),
            bestWaste = newBestWaste,
            currentSelection = currentSelection :+ utxo,
            bestSelection = newBestSelection,
            tries = tries + 1
          )
        }
      }
    }

    loop(
      currentValue = Satoshis.zero,
      currentAvailValue = startingValue,
      currentWaste = CurrencyUnits.zero,
      bestWaste = Satoshis.max,
      currentSelection = Vector.empty,
      bestSelection = Vector.empty,
      tries = 0
    )
  }

  /** Randomly selects utxos until it has enough to fund the desired amount,
    * should only be used for research purposes
    */
  def randomSelection(
      walletUtxos: Vector[CoinSelectorUtxo],
      outputs: Vector[TransactionOutput],
      feeRate: FeeUnit): Vector[CoinSelectorUtxo] = {
    val randomUtxos = Random.shuffle(walletUtxos)

    accumulate(randomUtxos, outputs, feeRate)
  }

  /** Greedily selects from walletUtxos starting with the largest outputs, skipping outputs with values
    * below their fees. Better for high fee environments than accumulateSmallestViable.
    */
  def accumulateLargest(
      walletUtxos: Vector[CoinSelectorUtxo],
      outputs: Vector[TransactionOutput],
      feeRate: FeeUnit): Vector[CoinSelectorUtxo] = {
    val sortedUtxos =
      walletUtxos.sortBy(_.prevOut.value).reverse

    accumulate(sortedUtxos, outputs, feeRate)
  }

  /** Greedily selects from walletUtxos starting with the smallest outputs, skipping outputs with values
    * below their fees. Good for low fee environments to consolidate UTXOs.
    *
    * Has the potential privacy breach of connecting a ton of UTXOs to one address.
    */
  def accumulateSmallestViable(
      walletUtxos: Vector[CoinSelectorUtxo],
      outputs: Vector[TransactionOutput],
      feeRate: FeeUnit): Vector[CoinSelectorUtxo] = {
    val sortedUtxos = walletUtxos.sortBy(_.prevOut.value)

    accumulate(sortedUtxos, outputs, feeRate)
  }

  /** Greedily selects from walletUtxos in order, skipping outputs with values below their fees */
  def accumulate(
      walletUtxos: Vector[CoinSelectorUtxo],
      outputs: Vector[TransactionOutput],
      feeRate: FeeUnit): Vector[CoinSelectorUtxo] = {
    val totalValue = outputs.foldLeft(CurrencyUnits.zero) {
      case (totVal, output) => totVal + output.value
    }

    @tailrec
    def addUtxos(
        alreadyAdded: Vector[CoinSelectorUtxo],
        valueSoFar: CurrencyUnit,
        bytesSoFar: Long,
        utxosLeft: Vector[CoinSelectorUtxo]): Vector[CoinSelectorUtxo] = {
      val fee = feeRate * bytesSoFar
      if (valueSoFar > totalValue + fee) {
        alreadyAdded
      } else if (utxosLeft.isEmpty) {
        throw new RuntimeException(
          s"Not enough value in given outputs ($valueSoFar) to make transaction spending $totalValue plus fees $fee")
      } else {
        val nextUtxo = utxosLeft.head
        val effectiveValue = calcEffectiveValue(nextUtxo, feeRate)
        if (effectiveValue <= Satoshis.zero) {
          addUtxos(alreadyAdded, valueSoFar, bytesSoFar, utxosLeft.tail)
        } else {
          val newAdded = alreadyAdded.:+(nextUtxo)
          val newValue = valueSoFar + nextUtxo.prevOut.value
          val approxUtxoSize = CoinSelector.approximateUtxoSize(nextUtxo)

          addUtxos(newAdded,
                   newValue,
                   bytesSoFar + approxUtxoSize,
                   utxosLeft.tail)
        }
      }
    }

    addUtxos(Vector.empty, CurrencyUnits.zero, bytesSoFar = 0L, walletUtxos)
  }

  def calculateUtxoFee(
      utxo: CoinSelectorUtxo,
      feeRate: FeeUnit): CurrencyUnit = {
    val approxUtxoSize = CoinSelector.approximateUtxoSize(utxo)
    feeRate * approxUtxoSize
  }

  def calcEffectiveValue(
      utxo: CoinSelectorUtxo,
      feeRate: FeeUnit): CurrencyUnit = {
    val utxoFee = calculateUtxoFee(utxo, feeRate)
    utxo.prevOut.value - utxoFee
  }
}

object CoinSelector extends CoinSelector {

  /** Cribbed from [[https://github.com/bitcoinjs/coinselect/blob/master/utils.js]] */
  def approximateUtxoSize(utxo: CoinSelectorUtxo): Long = {
    val inputBase = 32 + 4 + 1 + 4
    val scriptSize = utxo.redeemScriptOpt match {
      case Some(script) => script.bytes.length
      case None =>
        utxo.scriptWitnessOpt match {
          case Some(script) => script.bytes.length
          case None =>
            utxo.prevOut.scriptPubKey match {
              case _: NonWitnessScriptPubKey        => 107 // P2PKH
              case _: WitnessScriptPubKeyV0         => 107 // P2WPKH
              case _: TaprootScriptPubKey           => 64 // Single Schnorr signature
              case _: UnassignedWitnessScriptPubKey => 0 // unknown
            }
        }
    }

    inputBase + scriptSize
  }

  def selectByAlgo(
      coinSelectionAlgo: CoinSelectionAlgo,
      walletUtxos: Vector[CoinSelectorUtxo],
      outputs: Vector[TransactionOutput],
      feeRate: FeeUnit,
      changeCostOpt: Option[CurrencyUnit] = None,
      longTermFeeRateOpt: Option[FeeUnit] = None): Vector[CoinSelectorUtxo] =
    coinSelectionAlgo match {
      case RandomSelection =>
        randomSelection(walletUtxos, outputs, feeRate)
      case AccumulateLargest =>
        accumulateLargest(walletUtxos, outputs, feeRate)
      case AccumulateSmallestViable =>
        accumulateSmallestViable(walletUtxos, outputs, feeRate)
      case StandardAccumulate =>
        accumulate(walletUtxos, outputs, feeRate)
      case BranchAndBound =>
        (longTermFeeRateOpt, changeCostOpt) match {
          case (Some(longTermFeeRate), Some(changeCost)) =>
            selectCoinsBnB(walletUtxos,
                           outputs,
                           changeCost,
                           feeRate,
                           longTermFeeRate)
          case (None, None) | (Some(_), None) | (None, Some(_)) =>
            throw new IllegalArgumentException(
              "longTermFeeRateOpt must be defined for LeastWaste")
        }
      case LeastWaste =>
        (longTermFeeRateOpt, changeCostOpt) match {
          case (Some(longTermFeeRate), Some(changeCost)) =>
            selectByLeastWaste(walletUtxos,
                               outputs,
                               feeRate,
                               changeCost,
                               longTermFeeRate)
          case (None, None) | (Some(_), None) | (None, Some(_)) =>
            throw new IllegalArgumentException(
              "longTermFeeRateOpt must be defined for LeastWaste")
        }
      case SelectedUtxos(outPoints) =>
        val result = walletUtxos.foldLeft(Vector.empty[CoinSelectorUtxo]) {
          (acc, utxo) =>
            val outPoint = (utxo.outPoint.txId, utxo.outPoint.vout.toInt)
            if (outPoints(outPoint)) acc :+ utxo else acc
        }
        if (result.toSet.size < outPoints.size) {
          outPoints.foreach { outPoint =>
            if (
              !result.exists(utxo =>
                utxo.outPoint.txId == outPoint._1 && utxo.outPoint.vout.toInt == outPoint._2)
            )
              throw new IllegalArgumentException(
                s"Unknown UTXO: ${outPoint._1}:${outPoint._2}")
          }
        } else if (result.size > outPoints.size) {
          throw new IllegalArgumentException(s"Found too many UTXOs")
        }
        result
    }

  private case class CoinSelectionResults(
      waste: CurrencyUnit,
      totalSpent: CurrencyUnit,
      selection: Vector[CoinSelectorUtxo])

  implicit
  private val coinSelectionResultsOrder: Ordering[CoinSelectionResults] = {
    case (a: CoinSelectionResults, b: CoinSelectionResults) =>
      if (a.waste == b.waste) {
        a.selection.size.compare(b.selection.size)
      } else a.waste.compare(b.waste)
  }

  def selectByLeastWaste(
      walletUtxos: Vector[CoinSelectorUtxo],
      outputs: Vector[TransactionOutput],
      feeRate: FeeUnit,
      changeCost: CurrencyUnit,
      longTermFeeRate: FeeUnit
  ): Vector[CoinSelectorUtxo] = {
    val target = outputs.map(_.value).sum
    val results = CoinSelectionAlgo.independentAlgos.flatMap { algo =>
      // Skip failed selection attempts
      Try {
        val selection =
          selectByAlgo(algo,
                       walletUtxos,
                       outputs,
                       feeRate,
                       Some(changeCost),
                       Some(longTermFeeRate))

        val waste = calculateSelectionWaste(selection,
                                            Some(changeCost),
                                            target,
                                            feeRate,
                                            longTermFeeRate)

        val totalSpent = selection.map(_.prevOut.value).sum
        CoinSelectionResults(waste, totalSpent, selection)
      }.toOption
    }

    require(
      results.nonEmpty,
      s"Not enough value in given outputs to make transaction spending $target plus fees")

    results.min.selection
  }

  /** Compute the waste for this result given the cost of change
    * and the opportunity cost of spending these inputs now vs in the future.
    * If change exists, waste = changeCost + inputs * (effective_feerate - long_term_feerate)
    * If no change, waste = excess + inputs * (effective_feerate - long_term_feerate)
    * where excess = totalEffectiveValue - target
    * change_cost = effective_feerate * change_output_size + long_term_feerate * change_spend_size
    *
    * Copied from
    * @see https://github.com/achow101/bitcoin/blob/4f5ad43b1e05cd7b403f87aae4c4d42e5aea810b/src/wallet/coinselection.cpp#L345
    *
    * @param utxos The selected inputs
    * @param changeCostOpt The cost of creating change and spending it in the future. None if there is no change.
    * @param target The amount targeted by the coin selection algorithm.
    * @param longTermFeeRate The expected average fee rate used over the long term
    * @return The waste
    */
  def calculateSelectionWaste(
      utxos: Vector[CoinSelectorUtxo],
      changeCostOpt: Option[CurrencyUnit],
      target: CurrencyUnit,
      feeRate: FeeUnit,
      longTermFeeRate: FeeUnit): CurrencyUnit = {
    require(
      utxos.nonEmpty,
      "This function should not be called with empty inputs as that would mean the selection failed")

    val (waste, selectedEffectiveValue) =
      utxos.foldLeft((CurrencyUnits.zero, CurrencyUnits.zero)) {
        case ((waste, selectedEffectiveValue), utxo) =>
          val fee = calculateUtxoFee(utxo, feeRate)
          val longTermFee = calculateUtxoFee(utxo, longTermFeeRate)
          val effectiveValue = calcEffectiveValue(utxo, feeRate)

          val newWaste = waste + fee - longTermFee
          val newSelectedEffectiveValue =
            selectedEffectiveValue + effectiveValue

          (newWaste, newSelectedEffectiveValue)
      }

    changeCostOpt match {
      case Some(changeCost) =>
        // Consider the cost of making change and spending it in the future
        // If we aren't making change, the caller should've set changeCost to 0
        require(changeCost > Satoshis.zero,
                "Cannot have a change cost less than 1")
        waste + changeCost
      case None =>
        // When we are not making change (changeCost == 0), consider the excess we are throwing away to fees
        require(selectedEffectiveValue >= target)
        waste + selectedEffectiveValue - target
    }
  }
}
