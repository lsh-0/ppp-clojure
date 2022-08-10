(ns ppp.recommendations.core
  (:require
   [ppp.api-raml.interface :as api-raml]
   ))

(defn recommendation-list
  [type id & [kwargs]]
  (let [defaults {:content-type-list [api-raml/recommendations-list-v2]
                  :page 1
                  :per-page 20
                  :order "desc"
                  :api-key nil}]
    (api-raml/api-request (str "/recommendations/" type "/" id) (merge defaults kwargs))))
