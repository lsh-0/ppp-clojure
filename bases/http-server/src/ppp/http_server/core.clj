(ns ppp.http-server.core
  (:require
   [compojure.core :refer [defroutes GET]]
   [compojure.route :as route]
   [ring.adapter.jetty :as jetty]
   [ppp.lax.interface :as articles]
   [ppp.api-raml.interface :as api-raml]
   [ring.middleware.defaults :refer [wrap-defaults api-defaults]]
   ;;[ring.middleware.reload :refer [wrap-reload]]
   ;; this is fine for now, but probably a bit slow.
   [org.tobereplaced.http-accept-headers]
   ))

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
  (let [wildcard-defaults {"*" "application/json"
                           "*/*" "application/json"
                           "application/*" "application/json"}

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
  any requests for content (or content versions) that are not supported are not made."
  [handler]
  (fn [request]
    (let [accept-header (-> request :headers (get "accept"))
          content-type-list (parse-accept-header accept-header)]
      (if-not (some (set content-type-list) api-raml/supported-content-type-set)
        {:status 406
         :headers {}
         :body (str "Could not negotiate an acceptable content type for: %s" accept-header)})
      (handler (assoc request :content-type-list content-type-list)))))

;;

(defn debug-handler
  [page per-page order content-type-list]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body (str "page:" page "\nper-page:" per-page "\norder:" order "\naccepts:" content-type-list "\n" )})

;; the route needs to extract and parse the 'accepts' header
;; the route needs to extract pagination parameters
;; the route needs to pass the whole lot to the interface fn

(defn api-response-handler
  [f & rest]
  (let [resp (apply f rest)]
    (if-let [proxy-resp (:http-resp resp)]

      ;; two problems:
      ;; 1. proxying content is desirable
      ;; 2. coercion from json and decompression of request is desirable
      (assoc proxy-resp "Content-Length")
        
      {:status 200
       :body (:content resp)})))

(defroutes api-routes
  (GET "/articles" [page per-page order api-key :as {content-type-list :content-type-list}]
       (api-response-handler articles/article-list content-type-list page per-page order api-key))
  (GET "/articles/:id" [id] articles/article)
  (GET "/articles/:id/related" [id] articles/related-article-list)
  (GET "/articles/:id/versions" [id] articles/article-version-list)
  (GET "/articles/:id/versions/:version" [id version] articles/article-version))

(defn run-server
  "starts a http web server on port 3000, does not block thread by default."
  [opts]
  (let [repl-opts {:port 3000
                   :join? false}
        opts (merge repl-opts opts)
        routes (-> api-routes
                   (wrap-defaults api-defaults)
                   (acceptable-content-types-middleware))
        ]
    (jetty/run-jetty routes opts)))

(defn -main
  "starts a http web server on port 80"
  [& args]
  ;; make some options more visible. full set here:
  ;; - https://github.com/ring-clojure/ring/blob/1.9.4/ring-jetty-adapter/src/ring/adapter/jetty.clj
  (let [opts {;; blocks thread until server ends, rendering a REPL unusable.
              :join? true
              :port 80
              :host "localhost"
              :ssl? false
              :max-threads 50
              :min-threads 8
              }]
    (run-server opts)
    (System/exit 0)))
