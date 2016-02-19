(defproject net.phobot.datomic/seed "2.0.0"
  :description "Tool for loading seed data for testing into Datomic databases."
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/tools.cli "0.3.3"]
                 [net.phobot.datomic/migrator "2.1.1"]
                ]
  :main net.phobot.datomic.seed
  :license {:name "MIT License"
            :url "http://opensource.org/licenses/MIT"
            :year 2015
            :key "mit"}
  :url "https://github.com/psfblair/datomic-seed"                
  :aot :all)
