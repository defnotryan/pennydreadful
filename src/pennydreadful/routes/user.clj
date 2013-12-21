(ns pennydreadful.routes.user
  (:use compojure.core)
  (:require [cemerick.friend :as friend]
            [liberator.core :refer [defresource]]
            [pennydreadful.util :refer [not-nil? parse-long]]
            [pennydreadful.auth :as auth]
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
        inserted-project (data-project/insert-project! (:id authn) project)]
     {::project inserted-project}))

(defn projects-header-location [{:keys [request] :as ctx}]
  (when (#{:post} (:request-method request))
    (str "/project/" (get-in ctx [::project :id]))))

(defn projects-handle-created [ctx]
  (pr-str (::project ctx)))

(defresource projects-resource []
  :allowed-methods [:get :post]
  :available-media-types ["text/html"]
  :authorized? resource-authenticated?
  :handle-ok projects-handle-ok
  :post! projects-post!
  :location projects-header-location
  :handle-created projects-handle-created
  :handle-unauthorized (fn [ctx]
                         (friend/throw-unauthorized nil nil)))

(defn resource-mutation-allowed? [project-eid {:keys [request]}]
  (auth/user-eid-can-mutate-project-eid?
   (:id (friend/current-authentication request))
   project-eid))

(defn project-delete! [project-eid ctx]
  (data-project/delete-project! project-eid))

(defresource project-resource [project-eid]
  :allowed-methods [:delete] ; TODO :get :put
  :authorized? #(resource-mutation-allowed? project-eid %)
  :delete! #(project-delete! project-eid %))

(defroutes user-routes

  (GET "/" [] (projects-resource))

  (ANY "/project" [] (projects-resource))

  (ANY "/project/:project-eid" [project-eid] (project-resource (parse-long project-eid))))
