(ns ppp.api-raml.interface
  (:require
   [ppp.api-raml.http]
   [ppp.api-raml.specs]))

;; I consider this the super-type for the derived elife types.
;; If a client/user-agent says they support json, they can support application/vnd.elife.*+json
(def application-json ["application/json" {}])

(def related-article-list ["application/vnd.elife.article-related+json" {}])
(def related-article-list-v1 ["application/vnd.elife.article-related+json" {:version 1}])

(def article-history ["application/vnd.elife.article-history+json" {}])
(def article-history-v1 ["application/vnd.elife.article-history+json" {:version 1}])

(def article-list ["application/vnd.elife.article-list+json" {}])
(def article-list-v1 ["application/vnd.elife.article-list+json" {:version 1}])

(def recommendations-list ["application/vnd.elife.recommendations+json" {}])
(def recommendations-list-v1 ["application/vnd.elife.recommendations+json" {:version 1}])
(def recommendations-list-v2 ["application/vnd.elife.recommendations+json" {:version 2}])

(def poa ["application/vnd.elife.article-poa+json" {}])
(def poa-v2 ["application/vnd.elife.article-poa+json" {:version 2}])
(def poa-v3 ["application/vnd.elife.article-poa+json" {:version 3}])

(def vor ["application/vnd.elife.article-vor+json" {}])
(def vor-v5 ["application/vnd.elife.article-vor+json" {:version 5}])
(def vor-v6 ["application/vnd.elife.article-vor+json" {:version 6}])

(def supported-content-type-set
  #{application-json
    related-article-list related-article-list-v1
    article-history article-history-v1
    article-list article-list-v1
    poa poa-v2 poa-v3
    vor vor-v5 vor-v6})

;; ---

(def api-request ppp.api-raml.http/api-request)
