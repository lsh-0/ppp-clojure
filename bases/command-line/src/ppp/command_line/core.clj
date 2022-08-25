(ns ppp.command-line.core
  (:require
   [clojure.tools.namespace.repl :refer [refresh]]
   [clojure.test]
   [ppp.command-line.api :as api])
  (:gen-class))

(defn known-commands [] (->> 'ppp.command-line.api ns-interns keys (mapv str)))

(defn find-exec
  [arg-list]
  (let [cmd (some-> arg-list first (clojure.string/split #"/") last)
        fn-name (str "ppp.command-line.api/" cmd) ;; use full path, not alias
        kwargs {}
        fn-args (conj (vec (rest arg-list)) kwargs)]
    (if-let [api-fn (resolve (symbol fn-name))]
      (apply api-fn fn-args)
      (do (println "command not found:" cmd)
          (println "known commands:" (clojure.string/join ", " (known-commands)))))))

(defn test
  [& [ns-kw fn-kw]]
  ;;(clojure.tools.namespace.repl/refresh) ;; reloads all namespaces, including ppp.whatever-test ones
  ;;(utils/instrument true) ;; always test with spec checking ON

  (try
    ;; note! remember to update `cloverage.clj` with any new bindings
    (with-redefs [;;core/testing? true
                  ;;http/*default-pause* 1 ;; ms
                  ;;http/*default-attempts* 1
                  ;; don't pause while testing. nothing should depend on that pause happening.
                  ;; note! this is different to `joblib/tick-delay` not delaying when `joblib/*tick*` is unbound.
                  ;; tests still bind `joblib/*tick*` and run things in parallel.
                  ;;joblib/tick-delay joblib/*tick*
                  ;;main/spec? true
                  ;;cli/install-update-these-in-parallel cli/install-update-these-serially
                  ;;core/check-for-updates core/check-for-updates-serially
                  ;; for testing purposes, no addon host is disabled
                  ;;catalogue/host-disabled? (constantly false)
                  ]
      ;;(core/reset-logging!)

      (if ns-kw
        ;; todo: this static lookup could surely be improved
        (if (some #{ns-kw} [:lax.interface
                            :recommendations2.core
                            ])
          ;;(with-gui-diff
            (if fn-kw
              ;; `test-vars` will run the test but not give feedback if test passes OR test not found
              ;; slightly better than nothing
              (clojure.test/test-vars [(resolve (symbol (str "ppp." (name ns-kw) "-test") (name fn-kw)))])
              (clojure.test/run-all-tests (re-pattern (str "ppp." (name ns-kw) "-test")))
              ;;)
            )
          (println "unknown test file:" ns-kw))
        (clojure.test/run-all-tests #"ppp\..*-test")))
    (finally
      ;; use case: we run the tests from the repl and afterwards we call `restart` to start the app.
      ;; `stop` inside `restart` will be outside of `with-redefs` and still have logging `:min-level` set to `:debug`
      ;; it will dump a file and yadda yadda.
      ;;(core/reset-logging!)
      nil
      )))

(defn -main
  [& args]
  (println (find-exec args))
  (System/exit 0))
