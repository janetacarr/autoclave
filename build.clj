(ns build
  (:require [clojure.tools.build.api :as b]))

(def lib 'com.janetacarr/autoclave)
(def version "1.0.1")
(def class-dir "target/classes")
(def basis (b/create-basis {:project "deps.edn"}))
(def jar-file (format "target/%s-%s.jar" lib version))

(defn clean [_]
  (b/delete {:path "target"}))

(defn- pom-template [version]
  [[:description "A library for safely handling various kinds of user input. Forked from alxlit."]
   [:url "https://github.com/janetacarr/autoclave"]
   [:licenses
    [:license
     [:name "Eclipse Public License"]
     [:url "http://www.eclipse.org/legal/epl-v10.html"]]]
   [:developers
    [:developer
     [:name "Janet A. Carr"]]]
   [:scm
    [:url "https://github.com/janetacarr/autoclave"]
    [:connection "scm:git:https://github.com/janetacarr/autoclave.git"]
    [:developerConnection "scm:git:ssh://git@github.com/janetacarr/autoclave.git"]
    [:tag version]]])

(defn jar [_]
  (clean nil)
  (b/write-pom {:class-dir class-dir
                :lib lib
                :version version
                :basis basis
                :src-dirs ["src"]
                :pom-data (pom-template version)})
  (b/copy-dir {:src-dirs ["src"]
               :target-dir class-dir})
  (b/jar {:class-dir class-dir
          :jar-file jar-file}))

(defn install [_]
  (jar {})
  (b/install {:class-dir class-dir
              :lib lib
              :version version
              :basis basis
              :jar-file jar-file}))

(defn deploy
  [{:keys [repository] :as opts}]
  (let [dd-deploy (try (requiring-resolve 'deps-deploy.deps-deploy/deploy) (catch Throwable _))]
    (if dd-deploy
      (dd-deploy {:installer :remote
                  :artifact (b/resolve-path jar-file)
                  :repository (or (str repository) "clojars")
                  :pom-file (b/pom-path {:lib lib
                                         :class-dir class-dir})})
      (println "borked"))))
