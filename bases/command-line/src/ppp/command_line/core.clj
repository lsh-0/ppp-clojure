(ns ppp.command-line.core
  (:require
   [clojure.tools.namespace.repl :refer [refresh]]
   [clojure.test]
   [gui.diff :refer [with-gui-diff]]
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
  ;;(utils/instrument true) ;; always test with spec checking ON
  (if ns-kw
    ;; todo: this static lookup could surely be improved
    (if (some #{ns-kw} [:lax.interface
                        :recommendations2.core
                        ])
      (with-gui-diff
        (if fn-kw
          ;; `test-vars` will run the test but not give feedback if test passes OR test not found
          ;; slightly better than nothing
          (clojure.test/test-vars [(resolve (symbol (str "ppp." (name ns-kw) "-test") (name fn-kw)))])
          (clojure.test/run-all-tests (re-pattern (str "ppp." (name ns-kw) "-test")))
          )
        )
      (println "unknown test file:" ns-kw))
    (clojure.test/run-all-tests #"ppp\..*-test")))

(defn -main
  [& args]
  (println (find-exec args))
  (System/exit 0))
