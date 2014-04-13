(ns pennydreadful.routes.user
  (:use compojure.core)
  (:require [cemerick.friend :as friend]
            [liberator.core :refer [defresource]]
            [pennydreadful.util :refer [not-nil? parse-long parse-int parse-inst]]
            [pennydreadful.auth :as auth]
            [pennydreadful.data.datomic :as data]
            [pennydreadful.data.aggregation :as data-agg]
            [pennydreadful.data.user :as data-user]
            [pennydreadful.data.collection :as data-coll]
            [pennydreadful.data.project :as data-project]
            [pennydreadful.data.folder :as data-folder]
            [pennydreadful.data.snippet :as data-snippet]
            [pennydreadful.views.projects :as projects-view]
            [pennydreadful.views.project :as project-view]
            [pennydreadful.views.collection :as collection-view]))

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
  (auth/user-eid-can-mutate-eid?
   (:id (friend/current-authentication request))
   project-eid))

(defn project-put! [project-eid ctx]
  (let [project (-> ctx :request :params)
        safe-project (assoc project :id project-eid)]
    (data-project/update-project! safe-project)))

(defn project-delete! [project-eid ctx]
  (data-project/delete-project! project-eid))

(defn project-handle-ok [project-eid {:keys [request] :as ctx}]
  (let [authn (friend/current-authentication request)
        project (data-project/project-by-eid project-eid {:depth :snippet-meta})]
    (project-view/render {:project project :username (:username authn)})))

(defresource project-resource [project-eid]
  :allowed-methods [:delete :put :get]
  :available-media-types ["text/html"]
  :authorized? #(resource-mutation-allowed? project-eid %) ;; also enforces that project-eid exists
  :put! #(project-put! project-eid %)
  :delete! #(project-delete! project-eid %)
  :handle-ok #(project-handle-ok project-eid %)
  :handle-unauthorized (fn [ctx] friend/throw-unauthorized nil nil))


(defn collection-mutation-allowed? [collection-eid {:keys [request]}]
  (auth/user-eid-can-mutate-eid?
   (:id (friend/current-authentication request))
   collection-eid))

(defn collection-handle-ok [collection-eid {:keys [request] :as ctx}]
  (let [authn (friend/current-authentication request)
        collection (data-coll/collection-by-eid collection-eid {:depth :snippet})
        project-eid (data-coll/project-eid-for-collection-eid collection-eid)
        project (data-project/project-by-eid project-eid {:depth :snippet-meta})
        view-context {:project project :collection collection :username (:username authn)}]
    (collection-view/render (-> view-context
                                (assoc-in [:collection :word-count] (data-agg/word-count collection))
                                (assoc-in [:collection :auto-target] (data-agg/word-count-target-aggregated collection :automatic))
                                (assoc-in [:collection :auto-deadline] (data-agg/deadline-aggregated collection :automatic))))))

(defn collection-delete! [collection-eid ctx]
  (data-coll/delete-collection! collection-eid))

(defn parse-target [{target :target :as params}]
  (if target
    (update-in params [:target] parse-int)
    params))

(defn parse-word-count-mode [{word-count-mode :word-count-mode :as params}]
  (if word-count-mode
    (update-in params [:word-count-mode] keyword)
    params))

(defn parse-deadline-mode [{deadline-mode :deadline-mode :as params}]
  (if deadline-mode
    (update-in params [:deadline-mode] keyword)
    params))

(defn parse-deadline [{deadline :deadline :as params}]
  (if deadline
    (update-in params [:deadline] parse-inst)
    params))

(def params->collection
  (comp
   parse-target
   parse-word-count-mode
   parse-deadline-mode
   parse-deadline))

(defn collection-put! [collection-eid ctx]
  (let [collection (-> ctx :request :params params->collection)
        safe-collection (assoc collection :id collection-eid)]
    (data-coll/update-collection! safe-collection)))

(defresource collection-resource [collection-eid]
  :allowed-methods [:get :delete :put]
  :available-media-types ["text/html"]
  :authorized? #(collection-mutation-allowed? collection-eid %)
  :put! #(collection-put! collection-eid %)
  :delete! #(collection-delete! collection-eid %)
  :handle-ok #(collection-handle-ok collection-eid %)
  :handle-unauthorized (fn [ctx] (friend/throw-unauthorized nil nil)))

(defn collections-header-location [{:keys [request] :as ctx}]
  (when (#{:post} (:request-method request))
    (str "/collection/" (get-in ctx [::collection :id]))))

(defn collection-post! [project-eid {:keys [request] :as ctx}]
  (let [authn (friend/current-authentication request)
        collection (:params request)
        inserted-collection (data-coll/insert-collection! project-eid collection)]
    {::collection inserted-collection}))

(defn collections-handle-created [ctx]
  (pr-str (::collection ctx)))

(defresource project-collection-resource [project-eid]
  :allowed-methods [:post]
  :available-media-types ["text/html"]
  :authorized? #(resource-mutation-allowed? project-eid %)
  :post! #(collection-post! project-eid %)
  :location collections-header-location
  :handle-created collections-handle-created)

(defn collection-move-up! [collection-eid _]
  (data-coll/move-up! collection-eid)
  {::collection (data-coll/collection-by-eid collection-eid)})

(defresource collection-move-up [collection-eid]
  :allowed-methods [:put]
  :available-media-types ["text/html"]
  :authorized? #(collection-mutation-allowed? collection-eid %)
  :put! #(collection-move-up! collection-eid %)
  :handle-created collections-handle-created)

(defn collection-move-down! [collection-eid _]
  (data-coll/move-down! collection-eid)
  {::collection (data-coll/collection-by-eid collection-eid)})

(defresource collection-move-down [collection-eid]
  :allowed-methods [:put]
  :available-media-types ["text/html"]
  :authorized? #(collection-mutation-allowed? collection-eid %)
  :put! #(collection-move-down! collection-eid %)
  :handle-created collections-handle-created)

(defn folder-post! [collection-eid {:keys [request] :as ctx}]
  (let [folder (:params request)
        inserted-folder (data-folder/insert-folder-into-collection! collection-eid folder)]
    {::folder inserted-folder}))

(defn folder-header-location [{:keys [request] :as ctx}]
  (when (#{:post} (:request-method request))
    (str "/folder/" (get-in ctx [::folder :id]))))

(defn folder-handle-created [ctx]
  (pr-str (::folder ctx)))

(defresource collection-folder-resource [collection-eid]
  :allowed-methods [:post]
  :available-media-types ["text/html"]
  :authorized? (partial collection-mutation-allowed? collection-eid)
  :post! (partial folder-post! collection-eid)
  :location folder-header-location
  :handle-created folder-handle-created)

(defn folder-mutation-allowed? [folder-eid {:keys [request]}]
  (auth/user-eid-can-mutate-eid?
   (:id (friend/current-authentication request))
   folder-eid))

(defn- params->folder [params]
  params)

(defn folder-put! [folder-eid ctx]
  (let [folder (-> ctx :request :params params->folder)
        safe-folder (assoc folder :id folder-eid)]
    (data-folder/update-folder! safe-folder)))

(defresource folder-resource [folder-eid]
  :allowed-methods [:put]
  :available-media-types ["text/html"]
  :authorized? (partial folder-mutation-allowed? folder-eid)
  :put! (partial folder-put! folder-eid)
  :handle-unauthorized (fn [ctx] (friend/throw-unauthorized nil nil)))

(defn folder-move-up! [folder-eid _]
  (data-folder/move-up! folder-eid)
  {::folder (data-folder/folder-by-eid folder-eid)})

(defresource folder-move-up [folder-eid]
  :allowed-methods [:put]
  :available-media-types ["text/html"]
  :authorized? (partial folder-mutation-allowed? folder-eid)
  :put! (partial folder-move-up! folder-eid)
  :handle-created folder-handle-created)

(defn folder-move-down! [folder-eid _]
  (data-folder/move-down! folder-eid)
  {::folder (data-folder/folder-by-eid folder-eid)})

(defresource folder-move-down [folder-eid]
  :allowed-methods [:put]
  :available-media-types ["text/html"]
  :authorized? (partial folder-mutation-allowed? folder-eid)
  :put! (partial folder-move-down! folder-eid)
  :handle-created folder-handle-created)

(defn snippet-post! [collection-eid {:keys [request] :as ctx}]
  (let [snippet (-> request :params (assoc :content ""))
        inserted-snippet (data-snippet/insert-snippet-into-collection! collection-eid snippet)]
    {::snippet inserted-snippet}))

(defn snippet-header-location [{:keys [request] :as ctx}]
  (when (#{:post} (:request-method request))
    (str "/snippet/" (get-in ctx [::snippet :id]))))

(defn snippet-handle-created [ctx]
  (pr-str (::snippet ctx)))

(defresource collection-snippet-resource [collection-eid]
  :allowed-methods [:post]
  :available-media-types ["text/html"]
  :authorized? (partial collection-mutation-allowed? collection-eid)
  :post! (partial snippet-post! collection-eid)
  :location snippet-header-location
  :handle-created snippet-handle-created)

(defn snippet-mutation-allowed? [snippet-eid {:keys [request]}]
  (auth/user-eid-can-mutate-eid?
   (:id (friend/current-authentication request))
   snippet-eid))

(defn- params->snippet [params]
  params)

(defn snippet-put! [snippet-eid ctx]
  (let [snippet (-> ctx :request :params params->snippet)
        safe-snippet (assoc snippet :id snippet-eid)]
    (data-snippet/update-snippet! safe-snippet)))

(defresource snippet-resource [snippet-eid]
  :allowed-methods [:put]
  :available-media-types ["text/html"]
  :authorized? (partial snippet-mutation-allowed? snippet-eid)
  :put! (partial snippet-put! snippet-eid)
  :handle-unauthorized (fn [ctx] (friend/throw-unauthorized nil nil)))

(defroutes user-routes

  (GET "/" [] (projects-resource))

  (ANY "/project" [] (projects-resource))

  (ANY "/project/:project-eid" [project-eid] (project-resource (parse-long project-eid)))

  (ANY "/collection/:collection-eid" [collection-eid] (collection-resource (parse-long collection-eid)))

  (PUT "/collection/:collection-eid/move-up" [collection-eid] (collection-move-up (parse-long collection-eid)))

  (PUT "/collection/:collection-eid/move-down" [collection-eid] (collection-move-down (parse-long collection-eid)))

  (POST "/project/:project-eid/collection" [project-eid] (project-collection-resource (parse-long project-eid)))

  (POST "/collection/:collection-eid/folder" [collection-eid] (collection-folder-resource (parse-long collection-eid)))

  (POST "/collection/:collection-eid/snippet" [collection-eid] (collection-snippet-resource (parse-long collection-eid)))

  (PUT "/folder/:folder-eid" [folder-eid] (folder-resource (parse-long folder-eid)))

  (PUT "/folder/:folder-eid/move-up" [folder-eid] (folder-move-up (parse-long folder-eid)))

  (PUT "/folder/:folder-eid/move-down" [folder-eid] (folder-move-down (parse-long folder-eid)))

  (PUT "/snippet/:snippet-eid" [snippet-eid] (snippet-resource (parse-long snippet-eid))))
