/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

import CoreGraphics
import Foundation
import ImageIO
import UniformTypeIdentifiers

private let size = 48
private let red = CGColor(red: 0.94, green: 0.12, blue: 0.18, alpha: 1)
private let amber = CGColor(red: 1.0, green: 0.58, blue: 0.12, alpha: 1)
private let pale = CGColor(red: 1.0, green: 0.84, blue: 0.58, alpha: 1)

private func context() -> CGContext {
    let colorSpace = CGColorSpaceCreateDeviceRGB()
    let bitmapInfo = CGBitmapInfo(rawValue: CGImageAlphaInfo.premultipliedLast.rawValue)
    guard let result = CGContext(
        data: nil,
        width: size,
        height: size,
        bitsPerComponent: 8,
        bytesPerRow: size * 4,
        space: colorSpace,
        bitmapInfo: bitmapInfo.rawValue
    ) else {
        fatalError("Could not create drawing context")
    }
    result.setLineCap(.round)
    result.setLineJoin(.round)
    return result
}

private func stroke(_ points: [CGPoint], in context: CGContext, color: CGColor = red, width: CGFloat = 4) {
    guard let first = points.first else { return }
    context.beginPath()
    context.move(to: first)
    for point in points.dropFirst() { context.addLine(to: point) }
    context.setStrokeColor(color)
    context.setLineWidth(width)
    context.strokePath()
}

private func node(_ point: CGPoint, in context: CGContext, color: CGColor = amber, radius: CGFloat = 3.5) {
    context.setFillColor(color)
    context.fillEllipse(in: CGRect(x: point.x - radius, y: point.y - radius, width: radius * 2, height: radius * 2))
}

private func chevron(x: CGFloat, in context: CGContext, color: CGColor = pale) {
    stroke([CGPoint(x: x - 4, y: 31), CGPoint(x: x + 2, y: 24), CGPoint(x: x - 4, y: 17)], in: context, color: color, width: 3)
}

private func circuit(in context: CGContext) {
    stroke([
        CGPoint(x: 7, y: 12), CGPoint(x: 17, y: 12), CGPoint(x: 23, y: 18),
        CGPoint(x: 23, y: 31), CGPoint(x: 30, y: 38), CGPoint(x: 41, y: 38)
    ], in: context)
    node(CGPoint(x: 7, y: 12), in: context)
    node(CGPoint(x: 23, y: 24), in: context, radius: 3)
    node(CGPoint(x: 41, y: 38), in: context)
}

private func comparator(in context: CGContext, compact: Bool = false) {
    let tip: CGFloat = compact ? 27 : 34
    let output: CGFloat = compact ? 33 : 42
    stroke([CGPoint(x: 8, y: 12), CGPoint(x: tip, y: 24), CGPoint(x: 8, y: 36), CGPoint(x: 8, y: 12)], in: context, width: 3.5)
    stroke([CGPoint(x: tip, y: 24), CGPoint(x: output, y: 24)], in: context, color: pale, width: 3.5)
    node(CGPoint(x: 9, y: 12), in: context, radius: 3)
    node(CGPoint(x: 9, y: 36), in: context, radius: 3)
    node(CGPoint(x: compact ? 15 : 17, y: 24), in: context, color: pale, radius: 3)
    node(CGPoint(x: output, y: 24), in: context, radius: 3)
}

private func repeater(in context: CGContext) {
    context.setStrokeColor(red)
    context.setLineWidth(3.5)
    let body = CGPath(roundedRect: CGRect(x: 7, y: 13, width: 34, height: 22), cornerWidth: 6, cornerHeight: 6, transform: nil)
    context.addPath(body)
    context.strokePath()
    stroke([CGPoint(x: 15, y: 24), CGPoint(x: 29, y: 24)], in: context, color: pale, width: 3)
    stroke([CGPoint(x: 25, y: 19), CGPoint(x: 31, y: 24), CGPoint(x: 25, y: 29)], in: context, color: pale, width: 3)
    node(CGPoint(x: 11, y: 24), in: context, radius: 3)
    node(CGPoint(x: 37, y: 24), in: context, radius: 3)
}

private func draw(_ name: String, drawing: (CGContext) -> Void, outputDirectory: URL) {
    let drawingContext = context()
    drawing(drawingContext)
    guard let image = drawingContext.makeImage() else { fatalError("Could not render \(name)") }
    let destinationURL = outputDirectory.appendingPathComponent("\(name).png")
    guard let destination = CGImageDestinationCreateWithURL(destinationURL as CFURL, UTType.png.identifier as CFString, 1, nil) else {
        fatalError("Could not create \(destinationURL.path)")
    }
    CGImageDestinationAddImage(destination, image, nil)
    guard CGImageDestinationFinalize(destination) else { fatalError("Could not write \(destinationURL.path)") }
}

guard CommandLine.arguments.count == 2 else {
    fatalError("Usage: generate_wire_icons.swift OUTPUT_DIRECTORY")
}

let outputDirectory = URL(fileURLWithPath: CommandLine.arguments[1], isDirectory: true)
try FileManager.default.createDirectory(at: outputDirectory, withIntermediateDirectories: true)

draw("none", drawing: { drawingContext in
    stroke([CGPoint(x: 9, y: 9), CGPoint(x: 39, y: 39)], in: drawingContext, width: 5)
    stroke([CGPoint(x: 39, y: 9), CGPoint(x: 9, y: 39)], in: drawingContext, color: pale, width: 3)
}, outputDirectory: outputDirectory)

draw("normal", drawing: circuit, outputDirectory: outputDirectory)

draw("auto", drawing: { drawingContext in
    circuit(in: drawingContext)
    chevron(x: 34, in: drawingContext)
}, outputDirectory: outputDirectory)

draw("fast_auto", drawing: { drawingContext in
    circuit(in: drawingContext)
    chevron(x: 32, in: drawingContext)
    chevron(x: 39, in: drawingContext)
}, outputDirectory: outputDirectory)

draw("only_repeaters", drawing: repeater, outputDirectory: outputDirectory)

draw("only_comparators", drawing: { drawingContext in
    comparator(in: drawingContext)
}, outputDirectory: outputDirectory)

draw("fast_comparators", drawing: { drawingContext in
    comparator(in: drawingContext, compact: true)
    chevron(x: 42, in: drawingContext)
}, outputDirectory: outputDirectory)
