module.exports={
  "title": "bitcoin-s",
  "tagline": "Bitcoin implementation in Scala",
  "url": "https://bitcoin-s.org",
  "baseUrl": "/",
  "organizationName": "bitcoin-s",
  "projectName": "bitcoin-s",
  "scripts": [
    "https://buttons.github.io/buttons.js",
    "https://cdnjs.cloudflare.com/ajax/libs/clipboard.js/2.0.0/clipboard.min.js",
    "https://fonts.googleapis.com/css?family=Montserrat:500",
    "https://www.googletagmanager.com/gtag/js?id=UA-61958686-2",
    "/js/code-block-buttons.js"
  ],
  "stylesheets": [
    "/css/code-block-buttons.css"
  ],
  "favicon": "img/favicon.ico",
  "customFields": {
    "users": [
      {
        "caption": "Suredbits",
        "image": "/img/suredbits-logo.png",
        "infoLink": "https://suredbits.com",
        "description": "Suredbits uses Bitcoin-S to power their Lightning APIs.",
        "pinned": true
      },
      {
        "caption": "Gemini",
        "image": "/img/gemini-logo.png",
        "infoLink": "https://gemini.com",
        "description": "Gemini uses Bitcoin-S to batch transactions and facilitate deposits and withdrawals with full SegWit support. Read more at [their blog](https://medium.com/gemini/gemini-upgrades-wallet-with-full-support-of-segwit-5bb8e4bc851b)",
        "pinned": true
      }
    ],
    "fonts": {
      "headerFont": [
        "Montserrat",
        "sans-serif"
      ]
    },
    "separateCss": [
      "api"
    ],
    "repoUrl": "https://github.com/bitcoin-s/bitcoin-s",
    "suredbitsSlack": "https://join.slack.com/t/suredbits/shared_invite/zt-eavycu0x-WQL7XOakzQo8tAy7jHHZUw",
    "gitterUrl": "https://gitter.im/bitcoin-s-core/",
    "scaladocUrl": "/api/org/bitcoins"
  },
  "onBrokenLinks": "log",
  "onBrokenMarkdownLinks": "log",
  "presets": [
    [
      "@docusaurus/preset-classic",
      {
        "docs": {
          "path": "../docs",
          // "homePageId": "/core/core-intro", fixme
          "showLastUpdateAuthor": true,
          "showLastUpdateTime": true,
          "editUrl": "https://github.com/bitcoin-s/bitcoin-s/blob/master/docs/",
          "sidebarPath": "../website/sidebars.json"
        },
        "blog": {},
        "theme": {
          "customCss": "../src/css/customTheme.css"
        }
      }
    ]
  ],
  "plugins": [],
  "themeConfig": {
    "navbar": {
      "title": "bitcoin-s",
      "logo": {
        "src": "img/favicon.ico"
      },
      "items": [
        {
          "to": "docs/",
          "label": "Docs",
          "position": "left"
        },
        {
          "href": "download",
          "label": "Download",
          "position": "left"
        },
        {
          "href": "/api/org/bitcoins",
          "label": "API",
          "position": "left"
        },
        {
          "to": "/help",
          "label": "Help",
          "position": "left"
        },
        {
          "label": "Version",
          "to": "docs",
          "position": "right",
          "items": [
            {
              "label": "0.5.0",
              "to": "docs/",
              "activeBaseRegex": "docs/(?!0.1.0|0.2.0|0.3.0|0.4.0|0.5.0|next)"
            },
            {
              "label": "0.4.0",
              "to": "docs/0.4.0/"
            },
            {
              "label": "0.3.0",
              "to": "docs/0.3.0/"
            },
            {
              "label": "0.2.0",
              "to": "docs/0.2.0/"
            },
            {
              "label": "0.1.0",
              "to": "docs/0.1.0/"
            },
            {
              "label": "Master/Unreleased",
              "to": "docs/next/",
              "activeBaseRegex": "docs/next/(?!support|team|resources)"
            }
          ]
        }
      ]
    },
    "image": "img/undraw_online.svg",
    "footer": {
      "links": [],
      "copyright": "Copyright © 2021 Suredbits & the bitcoin-s developers",
      "logo": {
        "src": "img/favicon.ico"
      }
    },
    "algolia": {
      "apiKey": "0a510688bf8448e19aeb380377d328d3",
      "indexName": "bitcoin-s"
    },
    "gtag": {
      "trackingID": "UA-61958686-2"
    }
  }
}
