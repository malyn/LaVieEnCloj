(defproject la-vie-en-cloj "0.1.0"
  :description "mondrian-based interactive artwork"
  :url "https://github.com/malyn/LaVieEnCloj"
  :license {:name "BSD"
            :url "http://www.opensource.org/licenses/BSD-3-Clause"
            :distribution :repo }

  :dependencies [[org.clojure/clojure "1.5.1"]
                 [com.cemerick/piggieback "0.0.4"]
                 [compojure "1.1.5"]
                 [mondrian "0.1.0"]
                 [prismatic/dommy "0.1.1"]
                 [ring "1.1.8"]
                 [rm-hull/monet "0.1.7"]]

  :min-lein-version "2.1.2"
  :plugins [[lein-cljsbuild "0.3.2"]
            [lein-ring "0.8.3"]]
  :hooks [leiningen.cljsbuild]
  :repl-options {:nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}

  :source-paths ["src/clj"]
  :resource-paths ["resources" "target/resources"]

  :cljsbuild
  {:builds
   {:dev
    {:source-paths ["src/cljs" "src/cljs-repl"]
     :compiler {:output-to "target/resources/public/js/la_vie_en_cloj_dev.js"}}

    :prod
    {:source-paths ["src/cljs"]
     :compiler {:output-to "target/resources/public/js/la_vie_en_cloj.js"
                :optimizations :advanced
                :externs ["externs/jquery-1.9.js"
                          "externs/jquery-ui.js"]
                :pretty-print false}}}}

  :ring {:handler la-vie-en-cloj.server/app}

  :main la-vie-en-cloj.server)
