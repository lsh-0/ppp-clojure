(ns ppp.api-raml.interface
  (:require
   [ppp.api-raml.http]
   [ppp.api-raml.specs]))

;; I consider this the super-type for the derived elife types.
;; If a client/user-agent says they support json, they can support application/vnd.elife.*+json
(def application-json ["application/json" {}])

(def related-article-list ["application/vnd.elife.article-related+json" {}])
(def related-article-list-v1 ["application/vnd.elife.article-related+json" {:version 1}])
(def related-article-list-content-type
  [related-article-list-v1 related-article-list])

(def article-history ["application/vnd.elife.article-history+json" {}])
(def article-history-v1 ["application/vnd.elife.article-history+json" {:version 1}])
(def article-history-content-type
  [article-history-v1 article-history])

(def article-list ["application/vnd.elife.article-list+json" {}])
(def article-list-v1 ["application/vnd.elife.article-list+json" {:version 1}])
(def article-list-content-type
  [article-list-v1 article-list])

(def poa ["application/vnd.elife.article-poa+json" {}])
(def poa-v2 ["application/vnd.elife.article-poa+json" {:version 2}])
(def poa-v3 ["application/vnd.elife.article-poa+json" {:version 3}])
(def poa-content-type
  [poa-v3 poa-v2 poa])

(def vor ["application/vnd.elife.article-vor+json" {}])
(def vor-v5 ["application/vnd.elife.article-vor+json" {:version 5}])
(def vor-v6 ["application/vnd.elife.article-vor+json" {:version 6}])
(def vor-content-type
  [vor-v6 vor-v5 vor])

(def recommendations-list ["application/vnd.elife.recommendations+json" {}])
(def recommendations-list-v1 ["application/vnd.elife.recommendations+json" {:version 1}])
(def recommendations-list-v2 ["application/vnd.elife.recommendations+json" {:version 2}])
(def recommendations-list-content-type
  [recommendations-list-v2 recommendations-list-v1 recommendations-list])

(def collections-list ["application/vnd.elife.collection-list+json" {}])
(def collections-list-v1 ["application/vnd.elife.collection-list+json" {:version 1}])
(def collections-list-content-type
  [collections-list-v1 collections-list])

(def collection ["application/vnd.elife.collection+json" {}])
(def collection-v1 ["application/vnd.elife.collection+json" {:version 1}])
(def collection-v2 ["application/vnd.elife.collection+json" {:version 2}])
(def collection-content-type
  [collection-v2 collection-v1 collection])

(def search ["application/vnd.elife.search+json" {}])
(def search-v1 ["application/vnd.elife.search+json" {:version 1}])
(def search-content-type
  [search-v1 search])

(def podcast-episode-list ["application/vnd.elife.podcast-episode-list+json" {}])
(def podcast-episode-list-v1 ["application/vnd.elife.podcast-episode-list+json" {:version 1}])
(def podcast-episode-list-content-type
  [podcast-episode-list-v1 podcast-episode-list])

(def supported-content-type-set
  "a complete set of all supported content types.
  the 'accept' header on all api requests to the http-server are checked against this and
  refused (406) if not found."
  (reduce into #{} [[application-json]
                    related-article-list-content-type
                    article-history-content-type
                    article-list-content-type
                    poa-content-type
                    vor-content-type

                    recommendations-list-content-type
                    collections-list-content-type
                    collection-content-type
                    search-content-type
                    podcast-episode-list-content-type
                    ]))

;; ---

(def api-request ppp.api-raml.http/api-request)
(def api-response? ppp.api-raml.http/api-response?)
(def handle-api-response ppp.api-raml.http/handle-api-response)
(def http-error-response ppp.api-raml.http/http-error-response)
