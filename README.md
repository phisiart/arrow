# Arrow

Constructing Dataflow Graphs with Function Composition Style

## Introduction

Arrow is a domain-specific language enabling users to create dataflow graphs easily in a declarative way.

## Roadmap

Arrow is being reconstructed and open-sourced. The following features are to be (re)implemented.

- [x] Type Checking
- [x] Graph Constructing
- [x] Graph Drawing
- [x] Parallel Runtime
- [ ] Distributed Execution Support

## Complete Example
```scala
import arrow._

object Example {
  def main(args: Array[String]) {
    val graph = new ArrowGraph
    import graph._
    
    val flow = Stream(0, 1, 2) |> ((_: Int) + 1) |> ((_: Int) - 1)
  }
}
```

## Connection Examples
### A Simple Flow
1. Create a producer node:

    ```scala
    val producer: Node[Int, Int] = ...
    ```
    
    or a function:
    
    ```scala
    val producer: Int => Int = ...
    ```
    
2. Create a consumer node:

    ```scala
    val consumer: Node[Int, Int] = ...
    ```
    
    or a function:
    
    ```scala
    val consumer: Int => Int = ...
    ```
3. Connect nodes and supply an input stream with the polymorphic `|>` operator:

    ```scala
    val flow = Stream(1, 2, 3) |> producer |> consumer
    ```
    
    Consumer would then output a stream of `Int`s.
    
    Note that producer/consumer can either be `Node`s or functions. The input/output types must match between connections.
    
    `|>` is associative:
    
    ```scala
    val flow = Stream(1, 2, 3) |> (producer |> consumer)
    ```
    You can also use `<|`:
    
    ```scala
    val flow = consumer <| producer <| Stream(1, 2, 3)
    ```

### Broadcast
1. Create a producer (a node or a function, here a function):

    ```scala
    val producer: _ => Int = ...
    ```
    
    `_` means the type is unimportant here, similarly hereinafter.
    
2. Create a bunch of consumers (`List` can also be any subtype of `Traversable`):

    ```scala
    val consumers: List[Int => _] = ...
    ```

3. Connect using just one line:

    ```scala
    producer |> consumers
    ```
    
    instead of:
    
    ```scala
    for (consumer <- consumers) {
        producer |> consumer
    }
    ```

### Merge
```scala
val producers: List[_ => Int] = ...
val consumer: Int => _ = ...
producers |> consumer
```

### Split
```scala
val producer: _ => List[Int] = ...
val consumers: List[Int => _] = ...
producer |> consumers
```

### Join
```scala
val producers: List[_ => Int] = ...
val consumer: List[Int] => _ = ...
producers |> consumer
```

### HSplit
```scala
import shapeless._ // for `HList`
val producer: _ => (Int :: Double :: HNil)
val consumers: (Int => _) :: (Double => _) :: HNil
producer |> consumers
```

### HJoin
```scala
import shapeless._
val producers: (_ => Int) :: (_ => Double) :: HNil
val consumer: (Int :: Double :: HNil) => _
producers |> consumer
```

### HMatch
```scala
import shapeless._
val producers: (_ => Int) :: (_ => Double) :: HNil
val consumers: (Int => _) :: (Double => _) :: HNil
produceres |> consumers
```

### Using Flows as Inputs/Outputs
```scala
import shapeless._
val flow_lhs: _ => String = ...
val flow_rhs: String => Int = ...
val flow = flow_lhs |> flow_rhs // inputs `_`, outputs `Int`

val producers                     = flow :: xx :: HNil
val consumers: (Int => _) :: HNil = ...

producers |> consumers
```