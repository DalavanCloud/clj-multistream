(defproject mvxcvi/multistream "0.7.2-SNAPSHOT"
  :description "Clojure implementation of the multistream standard."
  :url "https://github.com/multiformats/clj-multistream"
  :license {:name "Public Domain"
            :url "http://unlicense.org/"}

  :deploy-branches ["master"]
  :pedantic? :abort

  :dependencies
  [[org.clojure/clojure "1.9.0"]]

  :hiera
  {:vertical false
   :cluster-depth 2
   :ignore-ns #{clojure}
   :show-external false}

  :codox
  {:metadata {:doc/format :markdown}
   :source-uri "https://github.com/multiformats/clj-multistream/blob/master/{filepath}#L{line}"
   :output-path "target/doc/api"}

  :profiles
  {:repl
   {:source-paths ["dev"]
    :dependencies
    [[org.clojure/tools.namespace "0.2.11"]]}

   :coverage
   {:plugins [[lein-cloverage "1.0.10"]]
    :dependencies [[riddley "0.1.14"]]}})
