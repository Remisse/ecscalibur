# ecscalibur

## Introduzione

L'obiettivo di questo progetto è duplice: realizzare un framework che permetta di strutturare i propri progetti Scala secondo il pattern architetturale **ECS** (Entity Component System) e analizzare pregi e difetti di tale pattern in relazione ad approcci object-oriented.

Il framework, che prende il nome di *ecscalibur*, è ispirato a progetti già esistenti e affermati come [flecs](https://github.com/SanderMertens/flecs), scritto in C++, e [Unity DOTS](https://unity.com/dots), parte del motore grafico Unity.  
ECS, che verrà descritto più nel dettaglio nel capitolo successivo, permette di separare i dati dalla logica di business e massimizzare il riutilizzo di codice senza ricorrere ad astrazioni ad-hoc che, in determinati casi, potrebbero rivelarsi poco lungimiranti all'interno del design di software object-oriented e richiedere, nei casi peggiori, costosi refactoring. Scopo principale di questo progetto sarà dunque creare un framework che rispetti questa visione, offrendo al tempo stesso una API intuitiva da utilizzare e apprendere.  
ECS trae grande vantaggio in termini di prestazioni da una disposizone in memoria di entità e componenti effettuata secondo l'approccio *data-oriented*. Nei linguaggi che permettono di allocare oggetti sullo stack, questo fa sì che il principio di *cache locality* venga sfruttato al meglio.
In questo progetto, tale principio viene quasi totalmente meno poiché Scala, come qualsiasi altro linguaggio che si appoggi sulla Java Virtual Machine, non permette allocazioni esplicite sullo stack se non per i tipi primitivi.
È consentita l'allocazione di oggetti *off-heap*, ma questa possibilità non è stata esplorata, così come non sono state applicate numerose ottimizzazioni che avrebbero compromesso il design in modi poco manutenibili e che, in ogni caso, non sarebbero state pertinenti al corso e agli obiettivi di questo progetto d'esame.  

Per quanto riguarda la parte di analisi di ECS, verranno elencati i motivi per i quali ECS potrebbe rappresentare una buona scelta per il proprio software e verranno inoltre proposte metodologie per integrare questo pattern con il *test-driven development* e col pattern architetturale *MVC*. Il tutto verrà descritto con l'ausilio di due semplici programmi dimostrativi, entrambi con la stessa business logic di fondo ma scritti e progettati uno attorno a un'architettura a componenti in stile object-oriented, l'altro utilizzando il framework creato appositamente per questo progetto.

La relazione è suddivisa in tre parti: la prima, che va dal capitolo 1 al 4, riguarda esclusivamente la parte di design e sviluppo del framework; la seconda è compresa tra i capitoli 5 e 8 e si focalizza sull'analisi di ECS riassunta poc'anzi; la terza, comprensiva del solo capitolo 9, include considerazioni e giudizi personali riguardo allo svolgimento del progetto.

## Indice

1. [*ecscalibur*: requisiti](1_requisiti.md)
2. [Design architetturale](2_architettura.md)
3. [Design di dettaglio](3_design.md)
4. [Implementazione](4_implementazione.md)
5. [ECS vs OOP: perché scegliere ECS?](5_why_ecs.md)
6. [Approccio al design di software ECS](6_design_ecs.md)
7. [Approccio al TDD con ECS](7_tdd_ecs.md)
8. [Combinare MVC ed ECS](8_mvc_ecs.md)
9. [Retrospettiva](9_retrospettiva.md)

## Autore

[Bryan Corradino](https://github.com/Remisse)