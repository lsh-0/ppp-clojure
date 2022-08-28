(ns ppp.api-raml.http
  (:require
   [clj-http.client :as http]
   [orchestra.core :refer [defn-spec]]
   [clojure.spec.alpha :as s]
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
                                    (if-let [version (:version content-type-params)]
                                      (str content-type "; version=" version)
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

(defn-spec http-error? boolean?
  [http-resp map?]
  (-> http-resp :status (= 200) not))

(defn-spec api-response-error? boolean?
  [api-response :ppp.component/response]
  (-> api-response :http-resp http-error?))

(def http-resp-map
  "https://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html"
  {:4xx ["Client Error" "Client Error"]
   400 ["Bad Request" "The request could not be understood by the server due to malformed syntax."]
   404 ["Not Found" "The server has not found anything matching that URI."]
   406 ["Not Acceptable" "The server is not capable of generating a response that is acceptable according to the accept headers sent in the request."]

   :5xx ["Server Error" "Server Error"]
   
   })

(defn http-error-response
  "returns an internal error response using a title and description from `http-resp-map`"
  [http-resp & [overrides]]
  (let [status (:status http-resp)
        key (or (when (contains? http-resp-map status) status)
                (when (> status 399) :4xx)
                (when (> status 499) :5xx))
        
        [title description] (get http-resp-map key)

        resp {:http-resp (-> http-resp (dissoc :body) (dissoc :http-client))
              :content {:title title, :description description}
              :content-type "application/problem+json"
              :content-type-version nil
              :content-type-version-deprecated? false
              :authenticated? false}]
    (merge resp overrides)))

(defn-spec handle-api-response :ppp/pair-of-map+bool
  "convenience. given a successful response from calling `api-request`, returns a pair [http-body, true].
  if the response was unsuccessful, the pair returned is the full response and false."
  [api-response :ppp.component/response]
  (let [{:keys [content]} api-response]
    (if (api-response-error? api-response)
      [api-response true]
      [content false])))

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
                        :request {:debug? false
                                  :query-params {}}}

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

            ;; todo: capture exceptions, especially timeouts
            http-resp (http/get (api-url endpoint) http-kwargs)

            ;; warn: KONG-Authenticated is unique to lax
            authenticated? (-> http-resp :headers (get "KONG-Authenticated") string-to-boolean)
            
            ;; 'warning' ? not the best header around :( assume it's presence means deprecation
            deprecated? (-> http-resp :headers (get "warning") string-to-boolean)]

        (if (http-error? http-resp)
          (http-error-response http-resp {:authenticated? authenticated?})
          
          ;; todo: content-type-version should be an int
          (let [[content-type content-type-version] (-> http-resp
                                                        :headers
                                                        (get "Content-Type")
                                                        (clojure.string/split #"; "))

                content-type-version (some-> content-type-version (subs 8) Integer/parseInt)

                resp {;; highly practical to have access to the http response, if available.
                      ;; the http-client is an object and cannot be serialised.
                      ;; the body is re-attached at 'content'
                      :http-resp (-> http-resp (dissoc :body) (dissoc :http-client))
                      :content (:body http-resp)
                      ;; todo: make this a single map `:content-type {:mime ..., :version 1, :deprecated? false, ...}`
                      :content-type content-type
                      :content-type-version content-type-version
                      :content-type-version-deprecated? deprecated?
                      :authenticated? authenticated?}]

            (if-not (:decompress-body http-kwargs)
              ;; if request was both successful and 'proxied', replace 'http-resp' with the original http-resp.
              ;; the client (projects.http-server) can figure out the rest.
              (assoc resp :http-resp http-resp)
              resp)))))))
