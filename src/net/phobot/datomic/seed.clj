(ns net.phobot.datomic.seed
  (:gen-class)
  (:require [clojure.string :as str]
            [clojure.java.io :as io]
            [clojure.tools.cli :as cli]
            [io.rkn.conformity :as conformity]
            [datomic.api :as datomic]
            [net.phobot.datomic.migrator :as migrator]
            [net.phobot.datomic.edn.edn-lister :as edn-lister]
  ))

(defn- new-temp-id [symbol-id-table key]
  (let [ids (vals symbol-id-table)]
    (if (nil? ids) 
      1
      (+ 1 (apply max ids)))))

(defn custom-edn-handlers [symbol-id-table-atom symbol-partition-table-atom ]
  (let [keyid-handler (fn [[partition key]] 
                        (let [tempid (new-temp-id @symbol-id-table-atom key)]
                          (do
                            (swap! symbol-id-table-atom assoc key tempid)
                            (swap! symbol-partition-table-atom assoc key partition)
                            (symbol (str "#db/id[" partition " " tempid "]")))))
        lookupid-handler (fn [key] 
                          (let [tempid (key @symbol-id-table-atom)
                                partition (key @symbol-partition-table-atom)]
                              (if (nil? tempid)
                                  (throw (Exception. (str "Lookup failed on key" key)))
                                  (symbol (str "#db/id[" partition " " tempid "] ")))))
        lookupids-handler (fn [keys] (->> keys (map lookupid-handler) (into [])))]
    { 'db/keyid keyid-handler 
      'db/lookupid lookupid-handler
      'db/lookupids lookupids-handler
    }))

(defn- read-seed-data-file [symbol-id-table symbol-partition-table logger-fn filepath]
  (let [readers (custom-edn-handlers symbol-id-table symbol-partition-table)]
    (do
      (logger-fn "Loading data file:" filepath)
      (->> (io/reader filepath)
           (java.io.PushbackReader.)  
           (clojure.edn/read { :readers readers })))))

(defn transact-seed-data [connection seed-data-path logger-fn]
  (let [seed-files (edn-lister/list-edn-files seed-data-path)
        symbol-id-table (atom {})
        symbol-partition-table (atom {})
        reader-fn (partial read-seed-data-file symbol-id-table symbol-partition-table logger-fn)
        preprocessed-edn (->> (map reader-fn seed-files) (apply concat) (into []) (str))
        edn-data (clojure.edn/read-string {:readers *data-readers*} preprocessed-edn)]
    (datomic/transact connection edn-data)))

(defn seed-database [connection migration-path seed-data-resource-path logger-fn]
    (if (some? migration-path)
      (do (migrator/run-migrations connection migration-path logger-fn))
      :default)
    (transact-seed-data connection seed-data-resource-path logger-fn))

(defn recreate-and-seed-database [db-url migration-path seed-data-resource-path logger-fn]
    (if (some? migration-path)
      (do
        (logger-fn "Full path of migration directory:" (-> migration-path clojure.java.io/file .getAbsolutePath))
        (logger-fn "Deleting database:" db-url)
        (datomic/delete-database db-url))
        ; Migrations will create the database if it doesn't already exist.
      :default)
    (let [connection (datomic/connect db-url)]
      (seed-database connection migration-path seed-data-resource-path logger-fn)))

(def cli-options
  [ ["-d" "--data PATH" "Path of seed data files" :default "seed-data"]
    ["-s" "--schema-dir PATH" "Path of database migrations. If this option is set, the database will be dropped and recreated."]
    ["-h" "--help"]
  ])
    
(defn- usage [options-summary]
  (->> ["Loads seed data into the specified database, optionally recreating it first."
        ""
        "Usage: lein seed [options] database_url"
        "or use java on the main class: net.phobot.datomic.seed [options] database_url"
        ""
        "Options:"
        options-summary        
        ""
        "Database URL should be of form: datomic:dev://localhost:4334/hello"
        ""]
       (str/join \newline)
       (.println *err*)))
       
(defn- error-msg [errors]
 (str "The following errors occurred while parsing your command:\n\n"
      (str/join \newline errors)))
      
(defn- exit [status msg]
  (println msg)
  (System/exit status))
            
(defn -main [& args] 
  (let [{:keys [options arguments errors summary]} (cli/parse-opts args cli-options)]
    (cond
      (:help options) (exit 0 (usage summary))
      errors (exit 1 (error-msg errors))
      (empty? arguments) (exit 1 (usage summary))
      :else 
        (let [db-url (nth arguments 0)
              migration-path (:schema-dir options)
              seed-data-path (:data options)
              log-fn #(apply println %&)] 
          (do
            (log-fn "Full path of seed data directory:" (-> seed-data-path clojure.java.io/file .getAbsolutePath))
            (recreate-and-seed-database db-url migration-path seed-data-path log-fn)
            (datomic/shutdown true))))))
