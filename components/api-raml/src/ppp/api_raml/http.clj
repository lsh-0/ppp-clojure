(ns ppp.api-raml.http
  (:require
   [clj-http.client :as http]
   ))

(defn validate
  [schema kwargs]
  [true ""])

(defn invalid
  [schema kwargs]
  {:type "about:blank" :title "error: invalid arguments" :detail "..."})

(defn content-type-list-header
  "converts a list of content-type pairs into a string suitable for a HTTP 'Accept' header."
  [content-type-list]
  (clojure.string/join ", " (mapv (fn [[content-type content-type-params]]
                                    (if-let [v (:version content-type-params)]
                                      (str content-type "; version=" (:version content-type-params))
                                      content-type))
                                  content-type-list)))

(defn api-url
  [endpoint]
  (str "https://api.elifesciences.org" endpoint))

(defn string-to-boolean
  [string]
  (try
    (Boolean/valueOf string)
    (catch IllegalArgumentException exc
      false)))

(defn api-request
  [endpoint kwargs]
  (let [;; a super-set of all possible params.
        default-kwargs {:subject-list [] ;; trait, see 'subjected'
                        :containing-list [] ;; trait, see 'container'
                        :content-type-list [] ;; header, see 'accept'
                        :api-key nil
                        ;; trait, see 'paged'
                        :page nil
                        :per-page nil
                        :order nil
                        ;; further options for the HTTP request
                        :request {}}
        kwargs (merge default-kwargs kwargs)
        kwargs (select-keys kwargs (keys default-kwargs)) ;; prunes any unsupported kwargs

        ;; validate the kwargs we were given.
        schema {}
        [valid? validation-response] (validate schema kwargs)]

    (if-not valid?
      (invalid validation-response kwargs)

      (let [{:keys [content-type-list api-key subject-list containing-list page per-page order api-key]} kwargs
            headers {"Accept" (content-type-list-header content-type-list)}
            query-params {:page page
                          :per-page per-page
                          :order order
                          :subject subject-list
                          :containing containing-list
                          }
            ;; only pass query params if their values are non-nil.
            query-params (into {} (filter second query-params))
            
            http-defaults {:as :json
                           :decompress-body true
                           :throw-exceptions false
                           :headers headers
                           :query-params query-params
                           :multi-param-style :array ;; php-style, `[1 2 3]` => `"a[]=1&a[]=2&a[]=3"`
                           }
            http-kwargs (:request kwargs)
            http-kwargs (merge http-defaults http-kwargs)

            http-resp (http/get (api-url endpoint) http-kwargs)

            ;; warn: KONG-Authenticated is unique to lax
            authenticated? (-> http-resp :headers (get "KONG-Authenticated") string-to-boolean)
            ;; 'warning' ? not the best header around :( assume it's presence means deprecation
            deprecated? (-> http-resp :headers (get "warning") string-to-boolean)]

        (cond
          (> (:status http-resp) 499)
          ;; todo: content-type for server errors not defined
          {:http-resp http-resp :content "server error" :content-type "server error" :content-type-version 1 :authenticated? authenticated?}
          
          (> (:status http-resp) 399)
          ;; todo: content-type for client errors not defined
          {:http-resp http-resp :content "client error" :content-type "client error" :content-type-version 1 :authenticated? authenticated?}

          :else
          ;; todo: content-type parsing sucks, version should be an int
          (let [[content-type content-type-version] (-> http-resp
                                                        :headers
                                                        (get "Content-Type")
                                                        (clojure.string/split #"; "))
                resp {:content (:body http-resp)
                      :content-type content-type
                      :content-type-version content-type-version
                      :content-type-version-deprecated? deprecated?
                      :authenticated? authenticated?}]

            ;; if request was 'proxied', replace the 'content' with 'http-resp', caller can figure it out.
            (if-not (:decompress-body http-kwargs)
              (-> resp
                  (dissoc :content)
                  (assoc :http-resp http-resp))

              resp)))))))
