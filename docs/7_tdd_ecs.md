### Approccio al TDD con ECS

Uno degli svantaggi di ECS è la maggiore difficoltà nell'adottare il *test-driven development* rispetto ad approcci OO più classici. 

Se si prevede che alcuni Component debbano offrire metodi che ne facilitino l'utilizzo (come nel caso di `Position` e `Velocity` della demo ECS, entrambi basati sulla classe `Vector2`), allora è possibile procedere al loro sviluppo scrivendo test allo stesso modo in cui lo si farebbe in sistemi OOP. Se non si riesce ad anticipare i componenti di cui si avrà bisogno, lo si può fare direttamente durante la scrittura dei test dei System, estendendo il trait System e implementandone la logica. 

Il problema principale, però, risiede nella validazione dei System. Si supponga di aver scritto il System `StopSystem`, che aggiunge i Component `StoppedEvent` e `ResumeMovementIntention` e rimuove il Component `StopMovementIntention` come mostrato nel capitolo precedente, e di volerne testare il funzionamento come nel seguente esempio:

```scala
"StopSystem" should "work correctly" in:
  val fixture = Fixture()
  given world: World = fixture.world

  world.entity withComponents 
  (StopMovementIntention(Velocity(Vector2.zero)) :: fixture.baseComponents)
  world system StopSystem(modelPriority)

  // ???
```

dove `Fixture` contiene tutti gli oggetti necessari all'esecuzione del System:

```scala
final class Fixture:
  val world = World()
  val view = View.empty()
  val baseComponents =
    List(Position(Vector2.zero), Velocity(Vector2.zero), ecsdemo.components.Timer(0f))
  private var hasAddedAll = false
  private var hasRemovedAll = false

  def markAsSuccessfullyAdded(): Unit = hasAddedAll = true
  def markAsSuccessfullyRemoved(): Unit = hasRemovedAll = true

  def wasTestSuccessful: Boolean = hasAddedAll && hasRemovedAll 
```

Si vuole che il test passi se i Component che il System deve aggiungere siano stati aggiunti correttamente (segnalando il fatto tramite il metodo `markAsSuccessfullyAdded()`) e se i Component che il System deve rimuovere siano stati rimossi correttamente (usando il metodo `markAsSuccessfullyRemoved()`).

La soluzione qui proposta consiste nel creare due System "validatori", uno che selezioni i Component che si suppone siano stati aggiunti e un altro che escluda tutte le Entity che abbiano il Component che si suppone sia stato rimosso. Se entrambi i validatori vengono eseguiti, allora si potrà dire che il System da testare funzioni correttamente.

Completando la scrittura del test:

```scala
"StopSystem" should "work correctly" in:
  val fixture = Fixture()
  given world: World = fixture.world

  world.entity withComponents 
    (StopMovementIntention(Velocity(Vector2.zero)) :: fixture.baseComponents)
  world system StopSystem(priority = 1)

  world.system("validatorAdd"):
    query all: (_: Entity, _: StoppedEvent, _: ResumeMovementIntention) =>
      fixture.markAsSuccessfullyAdded()
  world.system("validatorRemove"):
    query none StopMovementIntention all: _ =>
      fixture.markAsSuccessfullyRemoved()

  world loop 2.times
  fixture.wasTestSuccessful should be(true)
```

Nel caso del framework *ecscalibur*, è necessario che World iteri due volte per via del fatto che le modifiche strutturali alle Entity vengono eseguite all'inizio di ogni nuova iterazione. Scrivendolo in questo modo, il test passa se `fixture.wasTestSuccessful` restituisce `true`, ovvero se entrambi i System validatori sono stati eseguiti almeno una volta.

Questo processo è stato replicato in modo più o meno simile anche per gli altri System della demo. È un modo piuttosto innaturale di scrivere test, e il codice che ne risulta è molto poco espressivo (e dunque opaco), ma la natura "isolata" dei System ECS non permette di fare altrimenti.

Pagina successiva: [Retrospettiva](8_retrospettiva.md)
