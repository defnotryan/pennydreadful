(ns pennydreadful.routes.user
  (:use compojure.core)
  (:require [cemerick.friend :as friend]
            [liberator.core :refer [defresource]]
            [pennydreadful.util :refer [not-nil?]]
            [pennydreadful.data.datomic :as data]
            [pennydreadful.views.projects :as projects-view]))


(defresource projects-resource []
  :available-media-types ["text/html"]
  :authorized? (fn [ctx]
                 (not-nil? (friend/current-authentication (:request ctx))))
  :handle-ok (fn [ctx]
               (let [authn (friend/current-authentication (:request ctx))
                     username (:username authn)
                     user (data/user-for-username username)
                     projects (data/projects-for-user user)]
                 (projects-view/render {:projects projects :username username})))
  :handle-unauthorized (fn [ctx]
                         (friend/throw-unauthorized nil nil)))

(defroutes user-routes

  (GET "/" [] (projects-resource))

  (GET "/project" [] (projects-resource)))
