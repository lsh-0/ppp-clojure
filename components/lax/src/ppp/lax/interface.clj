(ns ppp.lax.interface
  (:require
   [ppp.lax.core :as lax]
   [clojure.spec.alpha :as s]
   [orchestra.core :refer [defn-spec]]
   [ppp.api-raml.interface]
   ))

;; the default should be to decompress and coerce json to edn.
;; ppp.http-server.core passes in the further kwargs to prevent decompressing and decoding the response.

(defn-spec article-list :ppp.component/response
  ([kwargs map?]
   (lax/article-list kwargs))
  ([content-type-list :api-raml/content-type-list, page pos-int?, per-page pos-int?, order :api-raml/order, api-key string?, kwargs map?]
   (lax/article-list (merge kwargs {:content-type-list content-type-list
                                    :page page
                                    :per-page per-page
                                    :order order
                                    :api-key api-key}))))

(defn-spec article :ppp.component/response
  ([id :api-raml/manuscript-id, kwargs map?]
   (lax/article id kwargs))
  ([id :api-raml/manuscript-id, content-type-list :api-raml/content-type-list, api-key string?, kwargs map?]
   (lax/article id (merge kwargs {:content-type-list content-type-list, :api-key api-key}))))

(defn-spec related-article-list :ppp.component/response
  ([id :api-raml/manuscript-id, kwargs map?]
   (lax/related-article-list id kwargs))
  ([id :api-raml/manuscript-id, content-type-list :api-raml/content-type-list, api-key string?, kwargs map?]
   (lax/related-article-list id (merge kwargs {:content-type-list content-type-list, :api-key api-key}))))

(defn-spec article-version-list :ppp.component/response
  ([id :api-raml/manuscript-id, kwargs map?]
   (lax/article-version-list id kwargs))
  ([id :api-raml/manuscript-id, content-type-list :api-raml/content-type-list, api-key string?, kwargs map?]
   (lax/article-version-list id (merge kwargs {:content-type-list content-type-list :api-key api-key}))))

(defn-spec article-version :ppp.component/response
  ([id :api-raml/manuscript-id, version pos-int?, kwargs map?]
   (lax/article-version id version kwargs))
  ([id :api-raml/manuscript-id, version pos-int?, content-type-list :api-raml/content-type-list, api-key string?, kwargs map?]
   (lax/article-version id version (merge kwargs {:content-type-list content-type-list :api-key api-key}))))
