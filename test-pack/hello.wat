(module
  ;; Import WASI functions
  (import "wasi_snapshot_preview1" "fd_write" (func $fd_write (param i32 i32 i32 i32) (result i32)))
  (import "wasi_snapshot_preview1" "proc_exit" (func $proc_exit (param i32)))

  ;; Memory
  (memory (export "memory") 1)

  ;; Data: "Hello from WASM!\n"
  (data (i32.const 0) "Hello from WASM!\n")

  ;; iov structure at offset 32
  ;; iov_base = 0, iov_len = 17 (length of "Hello from WASM!\n")
  (data (i32.const 32) "\00\00\00\00")  ;; iov_base = 0
  (data (i32.const 36) "\11\00\00\00")  ;; iov_len = 17

  (func (export "_start")
    ;; fd_write(stdout=1, iovs=32, iovs_len=1, nwritten=48)
    (call $fd_write
      (i32.const 1)    ;; stdout
      (i32.const 32)   ;; iovs pointer
      (i32.const 1)    ;; number of iovs
      (i32.const 48)   ;; where to write nwritten
    )
    drop

    ;; exit(0)
    (call $proc_exit (i32.const 0))
  )
)
