(ns ppp.search.core
  (:require
   [ppp.api-raml.interface :as api-raml :refer [api-request]]))

(defn search
  [search-query search-order search-type-list & [kwargs]]
  (let [defaults {:content-type-list [api-raml/search-v1]
                  :subject-list []
                  :page 1
                  :per-page 20
                  :order "desc"
                  :api-key nil}
        query-params {:for search-query
                      :sort search-order
                      :type search-type-list}
        kwargs (merge defaults kwargs)
        kwargs (update-in kwargs [:request :query-params] merge query-params)]
    (api-request "/search" kwargs)))

