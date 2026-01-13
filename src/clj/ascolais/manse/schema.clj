(ns ascolais.manse.schema
  "Malli schemas for Manse effects and options.")

(def sql-params
  "SQL params vector: [sql-string & params]"
  [:cat :string [:* :any]])

(def execute-opts
  "Options for execute/execute-one effects."
  [:map
   [:builder-fn {:optional true} :any]
   [:return-keys {:optional true} [:or :boolean [:sequential :keyword]]]
   [:connectable {:optional true} :any]
   [:column-fn {:optional true} :any]
   [:table-fn {:optional true} :any]
   [:label-fn {:optional true} :any]
   [:qualifier-fn {:optional true} :any]])

(def transaction-opts
  "Options for with-transaction effect."
  [:map
   [:isolation {:optional true} [:enum :read-uncommitted :read-committed
                                 :repeatable-read :serializable]]
   [:read-only {:optional true} :boolean]
   [:rollback-only {:optional true} :boolean]])
