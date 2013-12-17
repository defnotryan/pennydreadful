(ns pennydreadful.routes.home
  (:use compojure.core)
  (:require [ring.util.response :as response]
            [cemerick.friend :as friend]
            [pennydreadful.views.login :as views-login]
            [pennydreadful.util :as util]))


(defroutes home-routes
  (GET "/login" [] (views-login/render {}))
  (GET "/about" [] (constantly "Give us a moment, love."))
  (friend/logout (ANY "/logout" request (response/redirect "/"))))
