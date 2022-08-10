(ns ppp.recommendations.interface
  (:require
   [ppp.recommendations.core :as recommendations]
   ))

(defn recommendation-list
  ([type id kwargs]
   (recommendations/recommendation-list type id kwargs))
  ([type id content-type-list page per-page order api-key kwargs]
   (recommendations/recommendation-list type id (merge kwargs {:content-type-list content-type-list
                                                               :page page
                                                               :per-page per-page
                                                               :order order
                                                               :api-key api-key}))))
