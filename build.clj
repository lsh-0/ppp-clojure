;; https://clojure.org/guides/tools_build
;; https://github.com/polyfy/polylith/blob/master/examples/doc-example/build.clj

(ns build
  (:require
   [clojure.java.io :as io]
   [clojure.tools.deps.alpha :as t]
   [clojure.tools.deps.alpha.util.dir :refer [with-dir]]
   [org.corfield.build :as bb]
   [clojure.tools.build.api :as b]))

;; roughly modelled on Leiningen, no place for it in deps.edn
(def project
  {:name "ppp"
   :version "0.0.2-unreleased"
   :description "Pretty Perfect Publishing (PPP)"
   :url "https://github.com/lsh-0/ppp"
   :licence {:name "GNU Affero General Public License (AGPL)"
             :url "https://www.gnu.org/licenses/agpl-3.0.en.html"}})

(defn get-project-aliases []
  (let [edn-fn (juxt :root-edn :project-edn)]
    (-> (t/find-edn-maps)
        (edn-fn)
        (t/merge-edns)
        :aliases)))

(defn ensure-project-root
  "Given a task name and a project name, ensure the project
   exists and seems valid, and return the absolute path to it."
  [task project]
  (let [project-root (str (System/getProperty "user.dir") "/projects/" project)]
    (when-not (and project
                   (.exists (io/file project-root))
                   (.exists (io/file (str project-root "/deps.edn"))))
      (throw (ex-info (str task " task requires a valid :project option") {:project project})))
    project-root))

(defn uberjar
  "Builds an uberjar for the specified project.
   Options:
   * :project - required, the name of the project to build,
   * :uber-file - optional, the path of the JAR file to build,
     relative to the project folder; can also be specified in
     the :uberjar alias in the project's deps.edn file; will
     default to target/PROJECT.jar if not specified.
   Returns:
   * the input opts with :class-dir, :compile-opts, :main, and :uber-file
     computed.
   The project's deps.edn file must contain an :uberjar alias
   which must contain at least :main, specifying the main ns
   (to compile and to invoke)."
  [{:keys [project uber-file] :as opts}]
  (let [project-root (ensure-project-root "uberjar" project)
        aliases      (with-dir (io/file project-root) (get-project-aliases))
        main         (-> aliases :uberjar :main)]
    (when-not main
      (throw (ex-info (str "the " project " project's deps.edn file does not specify the :main namespace in its :uberjar alias")
                      {:aliases aliases})))
    (binding [b/*project-root* project-root]
      (let [class-dir "target/classes"
            uber-file (or uber-file
                          (-> aliases :uberjar :uber-file)
                          (str "target/" project ".uber.jar"))
            opts      (merge opts
                             {:class-dir    class-dir
                              :compile-opts {:direct-linking true}
                              :main         main
                              :uber-file    uber-file})]
        (b/delete {:path class-dir})
        (bb/uber opts)
        (b/delete {:path class-dir})
        (println "wrote:" (str project-root "/" uber-file))
        opts))))
