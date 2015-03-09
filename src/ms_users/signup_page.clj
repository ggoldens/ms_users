(ns ms-users.signup-page
  (:require
    [com.stuartsierra.component :as c]
    [de.otto.tesla.stateful.routes :as routes]
    [de.otto.status :as status]
    [compojure.core :as compojure]
    [ring.util.codec :as codec]
    [clojure.walk :as walk]
    [clj-pgp.message :as pgp-msg]
    [clojure.string :as str]
    [thi.ng.crypto.core :refer :all]
    [clojure.java.io :as io]
    [clojure.data.json :as json :only [write-str]]
    [ms-users.db.core :as db]))

(defn getmap 
  "converts form-data in a map"
  [form-params]
  (walk/keywordize-keys
    (codec/form-decode form-params)))

(defn format-public-key
  "Recieve a Publick Key in ASCII format, and return an encoded public key format"
  [pbk]
   (def tmp_file (str "pbk" (rand-int 30000) ".tmp")) 
   (spit tmp_file pbk)
   (def public_key (public-key tmp_file))
   (io/delete-file tmp_file)
   public_key
  )

(defn register-user [form]
      (if-not (db/get-user (:username form))
        (do 
            (db/create-user {:username (:username form) :public_key (:username form)})
            true
        )
        false
      )
)

(defn encrypt-message [form]
  (def message (str "Welcome " (:username form) "! Your registration was successful!"))
  (def public_key (format-public-key (:public_key form)))
  (def encrypted-message
            (pgp-msg/encrypt
              message public_key
              :format :utf8
              :cipher :aes-256
              :compress :zip
              :armor true))
  encrypted-message)

(defn welcome-page 
   "return the welcome page encrypted with PGP"
   [body]
   (def form (getmap body))
   (if (register-user form)
      ;Register ok
      (encrypt-message form)
      ;User exists
      "user-exists"
   )
)

(defn signup-user [self body]
    {:status  200
     :headers {"Content-Type" "application/json"}
     :body    (json/write-str (welcome-page body) )})



(defrecord SignupPage []
  c/Lifecycle
  (start [self]
    (routes/register-routes (:routes self)
                            [(compojure/POST "/signup" {body :body} (signup-user self (slurp body)))])
    ;(app-status/register-status-fun (:app-status self)
      ;(fn [] (status/status-detail :example-page :ok "page is always fine")))
    self)
  (stop [self]
    self))

(defn new-signup-page [] (map->SignupPage {}))
