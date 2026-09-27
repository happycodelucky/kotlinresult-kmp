//
//  ContentView.swift
//  kotlinresult — shared SwiftUI, used by both the iOS and macOS samples.
//
//  Demonstrates the Swift side of KotlinResult: `import KotlinresultKit` brings in
//  `KotlinResult` plus the bundled Swift helpers — `init(value:)` /
//  `init(failure:)` to build one, `get()` to unwrap it (throwing the Kotlin
//  exception itself on failure), and `result(as:)` for a `Swift.Result`.
//

import SwiftUI
import KotlinresultKit

struct ContentView: View {
    @State private var greeting: String = ""

    var body: some View {
        VStack(spacing: 16) {
            Image(systemName: "swift")
                .font(.system(size: 48))
                .foregroundStyle(.tint)
            Text(greeting.isEmpty ? "…" : greeting)
                .font(.title2)
                .multilineTextAlignment(.center)
        }
        .padding()
        .onAppear {
            let ok = KotlinResult<NSString>(value: "Hello from KotlinResult")
            let failed = KotlinResult<NSString>(failure: KotlinThrowable(message: "boom"))

            let value: String = (try? ok.get()) ?? "…"
            switch failed.result(as: String.self) {
            case .success(let text): greeting = "\(value)\n\(text)"
            case .failure(let error): greeting = "\(value)\nfailed: \(error.localizedDescription)"
            }
        }
    }
}

#Preview {
    ContentView()
}
