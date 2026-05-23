import Foundation
import Speech
import AVFoundation

final class SpeechRecognizerService: NSObject {
    private let recognizer = SFSpeechRecognizer(locale: Locale(identifier: "ko-KR"))
    private let audioEngine = AVAudioEngine()
    private var request: SFSpeechAudioBufferRecognitionRequest?
    private var task: SFSpeechRecognitionTask?

    func start(onText: @escaping (String) -> Void, onError: @escaping (String) -> Void) {
        SFSpeechRecognizer.requestAuthorization { [weak self] status in
            guard status == .authorized else {
                onError("음성 인식 권한이 필요합니다.")
                return
            }
            AVAudioSession.sharedInstance().requestRecordPermission { granted in
                guard granted else {
                    onError("마이크 권한이 필요합니다.")
                    return
                }
                DispatchQueue.main.async {
                    self?.startEngine(onText: onText, onError: onError)
                }
            }
        }
    }

    func stop() {
        task?.cancel()
        task = nil
        request?.endAudio()
        request = nil
        if audioEngine.isRunning {
            audioEngine.stop()
            audioEngine.inputNode.removeTap(onBus: 0)
        }
    }

    private func startEngine(onText: @escaping (String) -> Void, onError: @escaping (String) -> Void) {
        stop()
        guard let recognizer, recognizer.isAvailable else {
            onError("현재 음성 인식을 사용할 수 없습니다.")
            return
        }

        do {
            let session = AVAudioSession.sharedInstance()
            try session.setCategory(.record, mode: .measurement, options: [.duckOthers])
            try session.setActive(true, options: .notifyOthersOnDeactivation)

            let request = SFSpeechAudioBufferRecognitionRequest()
            request.shouldReportPartialResults = true
            self.request = request

            let inputNode = audioEngine.inputNode
            let format = inputNode.outputFormat(forBus: 0)
            inputNode.installTap(onBus: 0, bufferSize: 1024, format: format) { buffer, _ in
                request.append(buffer)
            }

            audioEngine.prepare()
            try audioEngine.start()

            task = recognizer.recognitionTask(with: request) { result, error in
                if let text = result?.bestTranscription.formattedString, !text.isEmpty {
                    onText(text)
                }
                if let error {
                    onError(error.localizedDescription)
                }
            }
        } catch {
            onError(error.localizedDescription)
        }
    }
}
