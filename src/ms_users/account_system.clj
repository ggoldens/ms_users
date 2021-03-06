(ns ms-users.account-system
  (:require [de.otto.tesla.system :as system]
            [ms-users.signup-page :as signup-page]
            [com.stuartsierra.component :as c])
  (:gen-class))


(defn account-system [runtime-config]
  (-> (system/empty-system (merge {:name "account-system"} runtime-config))
      (assoc :signup-page
             (c/using (signup-page/new-signup-page) [:routes :app-status]))
      (c/system-using {:server [:signup-page]})))

(defn -main
  "starts up the production system."
  [& args]
  (system/start-system (account-system{})))

