# ecscalibur

## Introduzione

L'obiettivo di questo progetto è duplice: realizzare un framework che permetta di strutturare i propri progetti Scala secondo il pattern architetturale **ECS** (Entity Component System) e analizzare pregi e difetti di tale pattern in relazione ad approcci object-oriented.

Il framework, che prende il nome di *ecscalibur*, è ispirato a progetti già esistenti e affermati come [flecs](https://github.com/SanderMertens/flecs), scritto in C, e [Unity DOTS](https://unity.com/dots), scritto in C++/C# e parte del motore grafico Unity.  
ECS, che verrà descritto più nel dettaglio nel capitolo successivo, permette di separare i dati dalla logica di business e massimizzare il riutilizzo di codice senza ricorrere ad astrazioni ad-hoc che, in determinati casi, potrebbero rivelarsi poco lungimiranti all'interno del design di software object-oriented e richiedere, nei casi peggiori, costosi refactoring. Scopo principale di questo progetto sarà dunque creare un framework che rispetti questa visione, offrendo al tempo stesso una API intuitiva da utilizzare e apprendere.  

Per quanto riguarda la parte di analisi di ECS, verranno elencati i motivi per i quali ECS potrebbe rappresentare una buona scelta per il proprio software e verranno inoltre proposte metodologie per integrare questo pattern con il *test-driven development* e col pattern architetturale *MVC*. Il tutto verrà descritto con l'ausilio di due semplici programmi dimostrativi, entrambi con la stessa business logic di fondo ma scritti e progettati uno attorno a un'architettura a componenti in stile object-oriented, l'altro utilizzando il framework creato appositamente per questo progetto.

La relazione è suddivisa in tre parti: la prima, che va dal capitolo 1 al 4, riguarda esclusivamente la parte di design e sviluppo del framework; la seconda è compresa tra i capitoli 5 e 7 e si focalizza sull'analisi di ECS riassunta poc'anzi; la terza, comprensiva del solo capitolo 8, include considerazioni e giudizi personali riguardo allo svolgimento del progetto.

## Processo di sviluppo

Trattandosi di un progetto individuale, non è stato adottato il processo di sviluppo SCRUM né una qualsiasi altra tipologia di programmazione agile.

Il repository del progetto è stato organizzato come segue:

- lo sviluppo è avvenuto quasi totalmente sul branch `main`
  - non essendoci stata la necessità di evitare conflitti con altri programmatori durante lo sviluppo e avendo adottato un sistema di release automatico basato sui *tag* (spiegato a breve), usare più branch di sviluppo o addirittura aprire pull request per ogni nuova feature sarebbe stata una complicazione inutile e probabilmente controproducente
- il progetto è stato suddiviso in sei sottoprogetti
  - `core`: codice del framework *ecscalibur*
  - `ecsutil`: metodi e classi di utility da cui `core` dipende
  - `demo_ecs`: demo realizzata usando il framework *ecscalibur*
  - `demo_oop`: demo realizzata con approccio object-oriented
  - `demo_util`: classi e metodi condivisi tra le due demo
  - `benchmark`: piccolo benchmark che confronta le prestazioni delle due demo con diversi numeri di entità (usato per scopi personali e non menzionato nella relazione)
- è stato configurato `scalafmt` per adeguare il codice a uno stile e a una formattazione uniformi
- sono stati abilitati numerosi parametri di compilazione che garantissero la scrittura di codice pulito e sicuro, rendendo superflua l'aggiunta di strumenti di linting quali `WartRemover`:  
```
-deprecation
-feature
-language:higherKinds
-language:implicitConversions
-unchecked
-Wunused:implicits
-Wunused:explicits
-Wunused:imports
-Wunused:locals
-Wunused:params
-Wunused:privates
-Wvalue-discard
-Xkind-projector
-Ycheck:all 
```
- è stato utilizzato `ScalaFix` tramite il plugin SBT `sbt-scalafix` con i seguenti comandi:  
  `DisableSyntax LeakingImplicitClassVal NoAutoTupling RedundantSyntax RemoveUnused`
- è stato configurato un workflow di continuous integration tramite *GitHub Actions* per rilasciare in automatico i file JAR del framework e delle demo alla creazione di un nuovo *tag*
- è stato usato *Scoverage* per il calcolo della percentuale di code coverage tramite il plugin SBT `sbt-scoverage`, caricandone automaticamente i risultati su *Codecov* tramite CI
- sono stati creati due branch non di sviluppo
  - `coverage`: è un'esatta copia di `main` con alcune modifiche al file `build.sbt` per abilitare il coverage selettivo su determinati sottoprogetti
    - si è reso necessario per via di problemi prestazionali causati da `sbt-scoverage` riscontrati durante alcuni benchmark
    - il workflow `coverage.yaml` viene eseguito automaticamente a ogni push su questo branch
  - `docs`: contiene tutti i file Markdown che compongono la relazione di progetto

Per lo sviluppo del framework e della demo ECS è stato adottato il *test-driven development*.

## Indice

1. [*ecscalibur*: requisiti](1_requisiti.md)
2. [Design architetturale](2_architettura.md)
3. [Design di dettaglio](3_design.md)
4. [Implementazione](4_implementazione.md)
5. [ECS vs OOP: perché scegliere ECS?](5_why_ecs.md)
6. [Approccio al design di software ECS](6_design_ecs.md)
7. [Approccio al TDD con ECS](7_tdd_ecs.md)
8. [Retrospettiva](8_retrospettiva.md)

## Autore

[Bryan Corradino](https://github.com/Remisse)
