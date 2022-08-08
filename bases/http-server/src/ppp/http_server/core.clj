(ns ppp.http-server.core
  (:require
   [compojure.core :refer [GET]]
   [compojure.route]
   [ring.adapter.jetty :as jetty]
   [ppp.lax.interface :as articles]
   [ppp.api-raml.interface :as api-raml]
   [ring.middleware.defaults :refer [wrap-defaults api-defaults]]
   ;;[ring.middleware.reload :refer [wrap-reload]]
   ;; this is fine for now, but probably a bit slow.
   [org.tobereplaced.http-accept-headers]
   )
  (:gen-class))

(defn spy
  [x]
  (println "spy:" x)
  x)

(defn to-int
  "given any value `x`, converts it to an integer or returns `nil` if it can't be converted."
  [x]
  (if (int? x)
    x
    (try (Integer/valueOf (str x))
         (catch NumberFormatException nfe
           nil))))

(defn parse-accept-header
  [accept-header]
  (let [;; api gateway accepts '*/*' but not '*' ...
        ;; api gateway accepts 'application/*' but not 'application/json' ...
        ;; is this strictness, pedantry, an oversight, a bug or what?
        wildcard-defaults {"*" "*/*"}

        ;; "application/vnd.elife.article-list+json;version=1" =>
        ;; (["application/vnd.elife.article-list+json" {"version" "1"}])
        content-type-list (org.tobereplaced.http-accept-headers/parse-accept wildcard-defaults accept-header)

        supported-params {"version" to-int}
        scrub-param (fn [[k v]]
                      (let [f (get supported-params k)
                            new-v (f v)]
                        (when new-v
                          [(keyword k) (f v)])))
        
        scrub-param-map (fn [[content-type param-map]]
                          (let [param-map (select-keys param-map (keys supported-params))
                                ;; {"version" "1"} => {:version 1}
                                param-map (into {} (map scrub-param param-map))]
                            [content-type param-map]))]
    
    ;; (["application/vnd.elife.article-list+json" {"version" "1"}]) =>
    ;; (["application/vnd.elife.article-list+json" {:version 1}])
    (mapv scrub-param-map content-type-list)))

(defn acceptable-content-types-middleware
  "we know the types of content supported because of the api-raml.
  any requests for content (or specific versions of content) that are not supported are not propagated."
  [handler]
  (fn [request]
    (let [accept-header (-> request :headers (get "accept"))
          content-type-list (parse-accept-header accept-header)]
      (if-not (some (set content-type-list) api-raml/supported-content-type-set)
        {:status 406
         :headers {}
         :body (str "Could not negotiate an acceptable content type for: %s" accept-header)})
      (handler (assoc request :content-type-list content-type-list)))))

(defn api-response-handler
  [f & rest]
  (let [;; effectively 'proxies' the response through clj-http without decompression or coercion of body contents.
        ;; the response from the article interface will otherwise always return native EDN across implementations.
        kwargs {:request {:decompress-body false
                          :as nil}} ;; output coercion, not compojure destructuring magic
        resp (apply f (conj (vec rest) kwargs))]
    (if-let [proxy-resp (:http-resp resp)]
      proxy-resp

      {:status 200
       :body (:content resp)})))

;;

(defn landing-page-handler
  [req]
  {:status 200
   :body "api landing page"})

(def routes
  (compojure.core/routes

   (GET "/api" [] landing-page-handler)
   
   (GET "/api/articles" [page per-page order api-key :as {content-type-list :content-type-list}]
        (api-response-handler articles/article-list content-type-list page per-page order api-key))

   (GET "/api/articles/:id" [id api-key :as {content-type-list :content-type-list}]
        (api-response-handler articles/article id content-type-list api-key))
   
   (GET "/api/articles/:id/related" [id api-key :as {content-type-list :content-type-list}]
        (api-response-handler articles/related-article-list id content-type-list api-key))

   (GET "/api/articles/:id/versions" [id api-key :as {content-type-list :content-type-list}]
        (api-response-handler articles/article-version-list id content-type-list api-key))

   (GET "/api/articles/:id/versions/:version" [id version api-key :as {content-type-list :content-type-list}]
        (api-response-handler articles/article-version id version content-type-list api-key))

   (compojure.route/not-found "404 Not Found.")))

(defn run-server
  "starts a http web server on an unprivileged port, does not block thread by default.
  returns the server object that can be stopped with `.stop`."
  [opts]
  (let [repl-opts {:join? false
                   :port 8080
                   :host "localhost"
                   :ssl? false
                   :max-threads 50
                   :min-threads 8}
        opts (merge repl-opts opts)
        routing (-> routes
                    (wrap-defaults api-defaults)
                    (acceptable-content-types-middleware))
        ]
    (println "opts" opts)
    (jetty/run-jetty routing opts)))

(defn -main
  "starts a http web server on port 80"
  [& args]
  ;; full set of options:
  ;; - https://github.com/ring-clojure/ring/blob/1.9.4/ring-jetty-adapter/src/ring/adapter/jetty.clj
  (let [opts {;; blocks thread until server ends, rendering a REPL unusable.
              :join? true}]
    (run-server opts)
    (System/exit 0)))
