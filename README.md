akka-remotepingpong-latencytest
====

Play ping-pong with remote actor, collect latency statistic and generate plots in R (http://www.r-project.org/).

## Install

Install git

Install java7

Install R (http://www.r-project.org/)

Clone this repo

```
git clone https://github.com/olegsmith/akka-remotepingpong-latencytest.git
cd akka-remotepingpong-latencytest.git
```

## Collect statistic

Start pong actor

```
./sbt "run-main sample.remote.pingpong.Starter pong"
```

Start ping actor by command with options $host, $concurrency, $warmup_request_count, $request_count
```
./sbt "run-main sample.remote.pingpong.Starter ping $host $concurrency $warmup_request_count $request_count"
```

Start collecting data
```
./sbt "run-main sample.remote.pingpong.Starter ping localhost 10 10000 1000000"

./sbt "run-main sample.remote.pingpong.Starter ping localhost 100 10000 1000000"

./sbt "run-main sample.remote.pingpong.Starter ping localhost 1000 10000 1000000"

./sbt "run-main sample.remote.pingpong.Starter ping localhost 10000 100000 1000000"

./sbt "run-main sample.remote.pingpong.Starter ping localhost 100000 200000 1000000"
```

## Generate reports
```
Rscript src/main/r/stat.r
```

Analyze plots in stat directory