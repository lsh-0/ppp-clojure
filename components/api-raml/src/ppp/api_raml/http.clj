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

;; https://github.com/CircleCI-Archived/frontend/blob/04701bd314731b6e2a75c40085d13471b696c939/src-cljs/frontend/utils.cljs
(defn deep-merge* [& maps]
  (let [f (fn [old new]
            (if (and (map? old) (map? new))
              (merge-with deep-merge* old new)
              new))]
    (if (every? map? maps)
      (apply merge-with f maps)
      (last maps))))

(defn deep-merge
  "Merge nested maps. At each level maps are merged left to right. When all
  maps have a common key whose value is also a map, those maps are merged
  recursively. If any of the values are not a map then the value from the
  right-most map is chosen.
  E.g.:
  user=> (deep-merge {:a {:b 1}} {:a {:c 3}})
  {:a {:c 3, :b 1}}
  user=> (deep-merge {:a {:b 1}} {:a {:b 2}})
  {:a {:b 2}}
  user=> (deep-merge {:a {:b 1}} {:a {:b {:c 4}}})
  {:a {:b {:c 4}}}
  user=> (deep-merge {:a {:b {:c 1}}} {:a {:b {:e 2 :c 15} :f 3}})
  {:a {:f 3, :b {:e 2, :c 15}}}
  Each of the arguments to this fn must be maps:
  user=> (deep-merge {:a 1} [1 2])
  AssertionError Assert failed: (and (map? m) (every? map? ms))
  Like merge, a key that maps to nil will override the same key in an earlier
  map that maps to a non-nil value:
  user=> (deep-merge {:a {:b {:c 1}, :d {:e 2}}}
                     {:a {:b nil, :d {:f 3}}})
  {:a {:b nil, :d {:f 3, :e 2}}}"
  [& maps]
  (let [maps (filter identity maps)]
    (assert (every? map? maps))
    (apply merge-with deep-merge* maps)))

;; ---

(defn api-request
  [endpoint kwargs]
  (let [;; a super-set of all common kwargs. these get validated, if present.
        default-kwargs {:content-type-list [] ;; header, see 'accept'
                        :subject-list [] ;; trait, see 'subjected'
                        :containing-list [] ;; trait, see 'container'
                        :api-key nil
                        ;; trait, see 'paged'
                        :page nil
                        :per-page nil
                        :order nil
                        ;; further options for the HTTP request
                        :request {:query-params {}}}

        kwargs (deep-merge default-kwargs kwargs)
        kwargs (select-keys kwargs (keys default-kwargs)) ;; prunes any unsupported kwargs

        ;; validate the kwargs we were given.
        ;; todo: pull the schema from the api-raml based on the `endpoint` ?
        schema {}
        [valid? validation-response] (validate schema kwargs)]

    (if-not valid?
      (invalid validation-response kwargs)

      (let [{:keys [content-type-list subject-list containing-list page per-page order api-key]} kwargs
            headers {"Accept" (content-type-list-header content-type-list)}
            
            http-defaults {:as :json
                           :decompress-body true
                           :throw-exceptions false
                           :headers headers
                           :query-params {:page page
                                          :per-page per-page
                                          :order order
                                          :subject subject-list
                                          :containing containing-list}
                           :multi-param-style :array ;; php-style, `[1 2 3]` => `"a[]=1&a[]=2&a[]=3"`
                           }

            http-kwargs (deep-merge http-defaults (:request kwargs))
            
            ;; only pass query params if their values are non-nil.
            http-kwargs (update-in http-kwargs [:query-params] #(into {} (filter second %)))
            
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
