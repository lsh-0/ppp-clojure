(ns ppp.search.interface
  (:require
   [ppp.search.core :as core]))

(defn search
  ([search-query kwargs]
   (core/search search-query nil nil kwargs))

  ([search-query search-type-list kwargs]
   (core/search search-query search-type-list nil kwargs))
  
  ([search-query search-type-list search-order kwargs]
   (core/search search-query search-type-list search-order kwargs))

  ([search-query search-type-list search-order content-type-list subject-list containing-list page per-page order api-key kwargs]
   (core/search search-query search-order search-type-list (merge kwargs {:content-type-list content-type-list
                                                                          :subject-list subject-list
                                                                          :containing-list containing-list
                                                                          :page page
                                                                          :per-page per-page
                                                                          :order order
                                                                          :api-key api-key}))))
