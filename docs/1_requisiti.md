# *ecscalibur*: requisiti

## Modello di dominio

Il pattern architetturale Entity Component System ha come obiettivo la separazione dei dati dalla logica di business ([Mertens et al.](https://github.com/SanderMertens/ecs-faq)). Un tipico framework ECS deve offrire la possibilità di creare i seguenti elementi fondamentali:

- *Entity*: identificatori univoci che non mantengono alcuno stato né logica;
- *Component*: contenitori di dati le cui istanze sono associate a singole entità (ad esempio, la posizione di un'entità in uno spazio bidimensionale);
- *System*: parti indipendenti della logica di business che operano su un sottinsieme delle entità e dei loro componenti e che vengono eseguite ciclicamente.

Gli elementi sopra descritti vengono memorizzati in una particolare struttura dati chiamata *World*, che funge da tramite per la loro creazione e manipolazione, occupandosi al tempo stesso di orchestrare l'esecuzione dei System.

## Requisiti

### Business

1. Si vuole realizzare una libreria Scala che funga da framework per la creazione di software strutturati secondo l'architettura ECS (Entity Component System, descritta nel paragrafo successivo);
2. Il framework deve avere una API concisa e intuitiva, ma che permetta al tempo stesso di creare applicazioni anche molto complesse;
   1. A dimostrazione di ciò, andrà realizzato un programma di esempio che simuli il comportamento in uno spazio bidimensionale di oggetti con caratteristiche e logica eterogenee;
3. Lo sviluppo deve concludersi nell'arco di due mesi.

### Utente

Gli utilizzatori del framework devono avere la possibilità di:

1. creare ed eliminare Entity
2. creare Component, siano essi mutabili o immutabili
3. assegnare istanze dei Component alle Entity ed eventualmente rimuoverle
4. aggiornare i valori dei Component mutabili di una Entity
5. rimpiazzare le istanze dei Component immutabili di una Entity
6. creare System che iterino su sottoinsiemi delle Entity e dei loro Component oppure che eseguano indipendentemente dalle Entity
   1. nello specifico, deve essere possibile **escludere** Entity con **almeno uno** dei Component indicati (*noneOf*), **includere** quelle che abbiano **almeno uno** tra i Component indicati (*anyOf*) e quelle che abbiano **tutti** i Component indicati (*allOf*)
7. per ogni System, specificare le istruzioni da eseguire a ogni iterazione, quando viene interrotto e quando viene fatta riprendere la sua esecuzione
8. assegnare in modo arbitrario identificatori univoci ai System
9. definire l'ordine di esecuzione dei System
10. interrompere l'esecuzione di singoli System ed eventualmente riprenderla
11. creare World tramite cui poter eseguire le suddette operazioni
12. eseguire la logica della propria applicazione attraverso il World, scegliendo se far compiere a quest'ultimo un numero prestabilito di iterazioni o lasciare che iteri all'infinito
13. specificare il numero di iterazioni al secondo che l'applicazione dovrebbe eseguire
14. sapere quanti secondi sono trascorsi tra un'iterazione del World e un'altra

### Sistema

Il framework deve garantire che:

1. sia impossibile istanziare Entity o System senza un World
2. sia impossibile creare Entity con lo stesso ID o senza alcun Component
   1. sulla base di questo requisito, rimuovere l'ultimo Component di una Entity deve comportare l'eliminazione della Entity stessa
3. sia impossibile assegnare più istanze dello stesso tipo di Component a una Entity
4. sia impossibile creare più System con lo stesso identificatore
5. l'ordine di esecuzione dei System deciso dall'utente venga rispettato
6. sia possibile creare più System con lo stesso valore di priorità senza necessariamente stabilire un ordine di esecuzione prevedibile per essi

### Non funzionali

1. La sintassi per filtrare Entity e Component deve ricordare quella di un linguaggio dichiarativo SQL-like
2. Aggiunta e rimozione di Entity e Component non devono comportare un'elevata frammentazione della memoria: dunque, all'allocazione di memoria aggiuntiva va preferito il "riciclo" di eventuali strutture dati non pienamente utilizzate
3. Il tempo trascorso tra un'iterazione del World e un'altra deve essere misurato accuratamente: nello specifico, l'errore di misurazione deve essere pari o inferiore a 10^(-8) secondi.
4. Per quanto riguarda le strutture dati da impiegare per memorizzare Entity e Component, evitare di "reinventare la ruota" e selezionarne di già testate e affermate ispirandosi a framework esistenti

### Di implementazione

Andranno adottate le seguenti tecnologie:

- Scala 3
- ScalaTest

Pagina successiva: [Design architetturale](./2_architettura.md)
