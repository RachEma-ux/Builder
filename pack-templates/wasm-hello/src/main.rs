// Simple WASM hello world example
// This is a template for creating WASM packs for Builder

use std::io::{self, Write};

fn main() {
    println!("Hello from WASM Pack!");
    println!("This is a template pack for the Builder mobile orchestration system.");

    // Example of writing to stdout
    io::stdout().flush().unwrap();
}

// Example exported function that can be called from workflows
#[no_mangle]
pub extern "C" fn greet(name_ptr: *const u8, name_len: usize) -> i32 {
    unsafe {
        let name_slice = std::slice::from_raw_parts(name_ptr, name_len);
        if let Ok(name) = std::str::from_utf8(name_slice) {
            println!("Hello, {}!", name);
            0 // Success
        } else {
            -1 // Error
        }
    }
}
