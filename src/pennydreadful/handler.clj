(ns pennydreadful.handler
  (:require [compojure.core :as cj]
            [ring.middleware.params]
            [ring.middleware.keyword-params]
            [ring.middleware.nested-params]
            [pennydreadful.routes.home :refer [home-routes]]
            [pennydreadful.routes.user :refer [user-routes]]
            [pennydreadful.util :refer [pprint-str]]
            [noir.util.middleware :as middleware]
            [compojure.route :as route]
            [taoensso.timbre :as timbre]
            [com.postspectacular.rotor :as rotor]
            [selmer.parser :as parser]
            [environ.core :refer [env]]
            [ring.util.response :as resp]
            [cemerick.friend :as friend]
            (cemerick.friend [workflows :as workflows]
                             [credentials :as creds])))

(cj/defroutes app-routes
  (route/resources "/"))


(cj/defroutes not-found-routes
  (route/not-found "Not Found"))

(defn init
  "init will be called once when
   app is deployed as a servlet on
   an app server such as Tomcat
   put any initialization code here"
  []
  (timbre/set-config!
    [:appenders :rotor]
    {:min-level :info
     :enabled? true
     :async? false ; should be always false for rotor
     :max-message-per-msecs nil
     :fn rotor/append})

  (timbre/set-config!
    [:shared-appender-config :rotor]
    {:path "pennydreadful.log" :max-size (* 512 1024) :backlog 10})

  (if (env :selmer-dev) (parser/cache-off!))
  (timbre/info "pennydreadful started successfully"))

(defn destroy
  "destroy will be called when your application
   shuts down, put any clean up code here"
  []
  (timbre/info "pennydreadful is shutting down..."))

(defn template-error-page [handler]
  (if (env :selmer-dev)
    (fn [request]
      (try
        (handler request)
        (catch clojure.lang.ExceptionInfo ex
          (let [{:keys [type error-template] :as data} (ex-data ex)]
            (if (= :selmer-validation-error type)
              {:status 500
               :body (parser/render error-template data)}
              (throw ex))))))
    handler))

(def users {"ryan" {:username "ryan"
                    :password (creds/hash-bcrypt "Passw0rd!")
                    :roles #{::user}}
            "rhea" {:username "rhea"
                    :password (creds/hash-bcrypt "woof")
                    :roles #{::user}}})

(def routes
  (apply cj/routes
         [app-routes
          home-routes
          user-routes
          not-found-routes]))

(defn debug [handler]
  (fn [request]
    (timbre/info (pprint-str request))
    (handler request)))

(defn wrap [handler]
  (-> handler
      ring.middleware.session/wrap-session
      ring.middleware.keyword-params/wrap-keyword-params
      ring.middleware.nested-params/wrap-nested-params
      ring.middleware.params/wrap-params))

(defn auth [handler]
  (friend/authenticate
   handler
   {:allow-anon? true
    :login-uri "/login"
    :default-landing-uri "/"
    :redirect-on-auth? true
    :credential-fn (partial creds/bcrypt-credential-fn users)
    :workflows [(workflows/interactive-form)]}))

(def app
  (-> routes
      auth
      ;debug
      wrap))
