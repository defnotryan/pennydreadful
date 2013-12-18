(defproject pennydreadful "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/clojurescript "0.0-2120"]
                 [compojure "1.1.6"]
                 [ring-server "0.3.1"]
                 [liberator "0.10.0"]
                 [selmer "0.5.4"]
                 [com.taoensso/timbre "2.7.1"]
                 [com.postspectacular/rotor "0.1.0"]
                 [com.taoensso/tower "2.0.1"]
                 [markdown-clj "0.9.35"]
                 [environ "0.4.0"]
                 [clj-time "0.6.0"]
                 [expectations "1.4.56"]
                 [com.cemerick/friend "0.2.0"]
                 [com.datomic/datomic-free "0.8.4260"]
                 [flyingmachine/cartographer "0.1.1"]
                 [enlive "1.1.4"]
                 [enfocus "2.0.2"]]
  :repl-options {:init-ns pennydreadful.repl}
  :aot :all
  :plugins [[lein-ring "0.8.7"]
            [lein-cljsbuild "1.0.1"]
            [lein-environ "0.4.0"]]
  :hooks [lein.cljsbuild]
  :ring {:handler pennydreadful.handler/app
         :init    pennydreadful.handler/init
         :destroy pennydreadful.handler/destroy}
  :datomic {:schemas ["resources/datomic" ["schema.edn"
                                           "test-data.edn"]]}
  :profiles
  {:production {:ring {:open-browser? false
                       :stacktraces?  false
                       :auto-reload?  false}
                :cljsbuild {:builds [{:source-paths ["src-cljs"]
                                      :compiler {:output-to "resources/public/js/site.js"
                                                 :optimizations :advanced}}]}
                :datomic {:config "resources/datomic/free-transactor-template.properties"
                          :db-uri "datomic:free://localhost:4334/pennydreadful-db"}}
   :dev {:dependencies [[ring-mock "0.1.5"]
                        [ring/ring-devel "1.2.1"]]
         :cljsbuild {:builds [{:source-paths ["src-cljs"]
                               :compiler {:output-to "resources/public/js/site.js"
                                          :optimizations :whitespace
                                          :pretty-print true}}]}
         :datomic {:config "resources/datomic/free-transactor-template.properties"
                   :db-uri "datomic:free://localhost:4334/pennydreadful-db"}
         :env {:selmer-dev true}}}
  :min-lein-version "2.0.0")
