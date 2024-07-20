# ECS vs OOP: perché scegliere ECS?

Quando ci si chiede se ECS sia una buona scelta per il proprio software, probabilmente si sta affrontando almeno uno dei seguenti problemi di design:

- le entità del dominio sono estremamente eterogenee tra loro e con comportamenti mutabili a seconda del loro stato, inducendo lo sviluppatore a voler separare il più possibile i dati dalla logica
- a causa di prestazioni insufficienti, si sta vagliando l'ipotesi di adottare un approccio *data-oriented*, che consiste nello strutturare i dati in modo tale da ottimizzarne i tempi di accesso, spesso collocandoli in array contigui allocati sullo stack per massimizzare il principio di *cache locality* della CPU ([Fabian, 2013](https://www.dataorienteddesign.com/dodmain/))

Come già spiegato nel capitolo introduttivo di questa relazione, ECS (o meglio, un buon framework ECS) permette di risolvere entrambi i problemi, a patto però che il software sia in qualche modo adattabile a questa architettura. Ad esempio, il design di un'interfaccia grafica si basa tipicamente su un'architettura reattiva, con eventi e metodi di callback da eseguire al compimento di determinate azioni da parte dell'utente: è sicuramente riconvertibile in stile ECS, ma ciò non significa che ne risulterà un design intuitivo o generalmente migliore. Per questo motivo, è probabilmente una buona idea essere pragmatici, strutturando il dominio o parte di esso seguendo l'architettura ECS e realizzando altre parti del sistema, come l'interfaccia, in modo separato e con metodi differenti.

## Qualità del codice

La completa separazione tra dati e logica aumenta considerevolmente la **riusabilità** del codice: i System operano su tutte le Entity che abbiano una particolare combinazione di Component e non su specifiche implementazioni di Entity; dunque, aggiungere o rimuovere "pezzi" di logica equivale a modificare i Component di una Entity o ad aggiungere nuovi System, attività che richiedono solo un minimo sforzo di refactoring ([Mertens et al.](https://github.com/SanderMertens/ecs-faq)). Dal momento che i System tendono ad avere poche responsabilità e ad essere indipendenti gli uni dagli altri, possono essere riutilizzati con facilità all'interno di più progetti ([Mertens et al.](https://github.com/SanderMertens/ecs-faq)) e rispettano totalmente il *single responsibility principle*.

Il discorso appena fatto ha ulteriori implicazioni sulla qualità del codice: la facilità con cui è possibile modificare la logica delle entità e, quindi, di rispondere a cambi di requisiti (anche *distruttivi*) con ECS portano a scrivere codice assolutamente **poco** **rigido**, **immobile** o **viscoso**. Con l'aumento del numero di Component e di System, tuttavia, è probabile che il codice diventi sempre più **opaco**: diviene complicato capire cosa faccia una particolare Entity se non si tiene traccia in qualche modo dei Component che le vengono aggiunti dai vari System e, di conseguenza, dei System che potranno iterare su di essa e modificarne lo stato.

In ogni caso, esistono alcuni pattern architetturali che permettono a progetti puramente object-oriented di ottenere *quasi* lo stesso grado di riusabilità di ECS, come si vedrà nel capitolo successivo.

Pagina successiva: [Approccio al design di software ECS](6_design_ecs.md)