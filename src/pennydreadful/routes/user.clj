(ns pennydreadful.routes.user
  (:use compojure.core)
  (:require [cemerick.friend :as friend]
            [liberator.core :refer [defresource]]
            [pennydreadful.util :refer [not-nil?]]
            [pennydreadful.data.datomic :as data]
            [pennydreadful.data.user :as data-user]
            [pennydreadful.data.project :as data-project]
            [pennydreadful.views.projects :as projects-view]))

(def resource-authenticated? (comp not-nil? friend/current-authentication :request))

(defn projects-handle-ok [{:keys [request] :as ctx}]
  (let [authn (friend/current-authentication request)
        user-eid (:id authn)
        projects (data-project/projects-for-user-eid user-eid)]
    (projects-view/render {:projects projects :username (:username authn)})))

(defn projects-post! [{:keys [request] :as ctx}]
  (let [authn (friend/current-authentication request)
        project (:params request)
        inserted-project (data-project/insert-project (:id authn) project)]
     {::project inserted-project}))

(defn projects-header-location [{:keys [request] :as ctx}]
  (when (#{:post} (:request-method request))
    (str "/project/" (get-in ctx [::project]))))

(defresource projects-resource []
  :allowed-methods [:get :post]
  :available-media-types ["text/html"]
  :authorized? resource-authenticated?
  :handle-ok projects-handle-ok
  :post! projects-post!
  :location projects-header-location
  :handle-unauthorized (fn [ctx]
                         (friend/throw-unauthorized nil nil)))

(defroutes user-routes

  (GET "/" [] (projects-resource))

  (ANY "/project" [] (projects-resource)))
