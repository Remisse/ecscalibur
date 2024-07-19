# Architettura

Dai requisiti è stata ricavata l'architettura riassunta dal seguente diagramma delle classi:

![](https://www.plantuml.com/plantuml/svg/ZLDDZzem4BtxLqmvXUs2r1uZYfP-75QgggsWwg7jmUiCOCaVaJrP2RNyzzgnYGEY5GT8FFFclUTdvy4JTzHfnLxH7ZN2Ytnst12JXj1jkK3uRfrgu3S3UDsW8hwH6clu65_NAAnfV6oX8Kc7IbZTMXCMleOrqeyFf_MlQIP0mYhCV-hnVP4tpG3pfNSFuYg7GsrVyBQ9pQ75DEKFdb1NqLR_TWzj3KzEMdUoqVBEOvLm0KLEcbqMz9s-sb7MjXNLy3ayezIK7Lb9BYkbwAnP4tS1-xL3T-Zrd1Ne_QXYpLFqSAeSHrGh9_P8GIAnURapd3g7r_OR0J8CSi9gMqELmxdC4jujuPkH54sJtI7jqVNiHNL0z_L8wrTnlsTwqnhUMwi2u1X-Gd7lpm6mM6WatdUMou6g6V4yDdcyQ2VNO_IZeHurpZX5bK8nWQgYVzu0RWx_wofrKrZbS5ZwC2Pl9c1di_2Sp-yc-Sn7lRkXLmunjsUpPTvcAPHCs02_rBW1acdp3GoqoNN_VovphROf7XU1CyrUdEcVsOY36Kl1muf0MqyGYY2D9su4Wjq2_sa6RBuXBRfiSWBSmvUtBArWFe-B0azhjfSDvb4XdL2OAkKYZAkesy2aKJ-Zyumze6awhVu2)

Più nel dettaglio, `World` è l'elemento cardine del framework: come da requisito utente 11, permette di eseguire tutte le operazioni di manipolazione su Entity e System specificate nel capitolo precedente.

La logica dei System è modellata attraverso l'interfaccia `Query`, che rappresenta costrutti simili alle query SQL e che verranno usate per filtrare le Entity e i loro Component. Il metodo `Query::all` termina la costruzione della Query.

Ogni specializzazione dell'interfaccia `Component` è caratterizzata da un ID univoco dato dalla metaclasse `ComponentType`. Questa scelta architetturale è stata influenzata sia dal requisito di avere una sintassi dichiarativa per le Query (requisito non funzionale 1), sia dalla decisione di utilizzare Scala come linguaggio di programmazione (requisito di implementazione 1): anticipando alcuni dettagli implementativi, si è voluto cercare un modo alternativo per costruire liste di classi di Component senza l'uso del metodo `classOf[T]`, rendendo sufficiente elencare direttamente i tipi `T` senza ricorrere ai generici e ottenendo così una sintassi più pulita. Siccome questa scelta ha influenzato parecchio sia l'architettura che il design di dettaglio del framework, sembrava giusto menzionarla il prima possibile.

*Archetype* (archetipo) rappresenta la struttura dati scelta per memorizzare le Entity e i loro Component. È la stessa utilizzata da `flecs` e `Unity DOTS`, i due framework a cui questo progetto è ispirato, e il fatto che sia una soluzione molto popolare in ambito ECS soddisfa il requisito non funzionale 4. Un Archetype memorizza solo le Entity che abbiano uno specifico insieme di Component; ad esempio, le Entity con i soli Component *Position* e *Velocity* verranno memorizzate in un Archetype apposito, mentre quelle con Position, Velocity e *Rotation* verranno memorizzate in un altro Archetype completamente slegato dal primo. Per una spiegazione più dettagliata, si faccia riferimento a questo [link](https://medium.com/@ajmmertens/building-an-ecs-2-archetypes-and-vectorization-fe21690805f9).

Il vincolo di una sola istanza di un tipo di Component per Entity (requisito di sistema 3) è stato espresso tramite una nota.

I restanti requisiti verranno affrontati nei capitoli successivi.

Pagina successiva: [Design di dettaglio](./3_design.md)
