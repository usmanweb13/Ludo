import wave
import math
import struct
import os

SAMPLE_RATE = 44100

def write_wav(filename, samples):
    with wave.open(filename, 'w') as wav_file:
        wav_file.setnchannels(1)  # Mono
        wav_file.setsampwidth(2)  # 16-bit
        wav_file.setframerate(SAMPLE_RATE)
        for s in samples:
            clamped = max(-1.0, min(1.0, s))
            val = int(clamped * 32767)
            wav_file.writeframes(struct.pack('<h', val))

def gen_dice_roll():
    # 5 impacts with decreasing intervals and jitter
    duration = 0.45
    total_samples = int(SAMPLE_RATE * duration)
    samples = [0.0] * total_samples
    impact_times = [0.0, 0.08, 0.17, 0.26, 0.34, 0.39]
    freqs = [380, 520, 440, 600, 490, 680]
    amps = [0.9, 0.8, 0.65, 0.5, 0.4, 0.3]

    for t_hit, freq, amp in zip(impact_times, freqs, amps):
        start_idx = int(t_hit * SAMPLE_RATE)
        hit_len = int(SAMPLE_RATE * 0.06)
        for i in range(hit_len):
            idx = start_idx + i
            if idx < total_samples:
                t = i / SAMPLE_RATE
                decay = math.exp(-t * 60.0)
                # Wood impact tone + broadband click
                tone = math.sin(2 * math.pi * freq * t) + 0.4 * math.sin(2 * math.pi * freq * 2.3 * t)
                noise = (math.sin(t * 123456.7) % 1.0) * 0.3
                samples[idx] += (tone + noise) * decay * amp
    return samples

def gen_token_step():
    # Resonant wooden tap
    duration = 0.14
    total_samples = int(SAMPLE_RATE * duration)
    samples = []
    freq = 780
    for i in range(total_samples):
        t = i / SAMPLE_RATE
        decay = math.exp(-t * 45.0)
        tone = math.sin(2 * math.pi * freq * t) * 0.7 + math.sin(2 * math.pi * (freq * 2.1) * t) * 0.3
        samples.append(tone * decay * 0.85)
    return samples

def gen_roll_six():
    # Joyful high arpeggio C6, E6, G6, C7
    duration = 0.55
    total_samples = int(SAMPLE_RATE * duration)
    samples = [0.0] * total_samples
    notes = [
        (0.0, 1046.5, 0.6, 0.25),
        (0.08, 1318.5, 0.7, 0.25),
        (0.16, 1567.9, 0.8, 0.28),
        (0.24, 2093.0, 0.9, 0.30)
    ]
    for start_t, freq, amp, note_dur in notes:
        start_idx = int(start_t * SAMPLE_RATE)
        len_idx = int(note_dur * SAMPLE_RATE)
        for i in range(len_idx):
            idx = start_idx + i
            if idx < total_samples:
                t = i / SAMPLE_RATE
                decay = math.exp(-t * 12.0)
                tone = math.sin(2 * math.pi * freq * t) + 0.3 * math.sin(2 * math.pi * freq * 2.0 * t)
                samples[idx] += tone * decay * amp * 0.5
    return samples

def gen_token_capture():
    # Punchy boing / capture impact
    duration = 0.4
    total_samples = int(SAMPLE_RATE * duration)
    samples = []
    for i in range(total_samples):
        t = i / SAMPLE_RATE
        # pitch slide down
        freq = 550.0 - 350.0 * (t / duration)
        decay = math.exp(-t * 14.0)
        tone = math.sin(2 * math.pi * freq * t) + 0.35 * math.sin(2 * math.pi * freq * 1.5 * t)
        samples.append(tone * decay * 0.9)
    return samples

def gen_victory():
    # Fanfare chords: C major progression
    duration = 1.3
    total_samples = int(SAMPLE_RATE * duration)
    samples = [0.0] * total_samples
    events = [
        # (start_t, [freqs], amp, duration)
        (0.0, [523.25, 659.25], 0.6, 0.25),         # C5, E5
        (0.25, [587.33, 783.99], 0.7, 0.25),        # D5, G5
        (0.50, [659.25, 830.61], 0.75, 0.25),       # E5, G#5 -> lead to A
        (0.75, [783.99, 1046.50, 1318.51], 0.9, 0.55) # G5, C6, E6 triumphant!
    ]
    for start_t, chord_freqs, amp, chord_dur in events:
        start_idx = int(start_t * SAMPLE_RATE)
        len_idx = int(chord_dur * SAMPLE_RATE)
        for i in range(len_idx):
            idx = start_idx + i
            if idx < total_samples:
                t = i / SAMPLE_RATE
                decay = math.exp(-t * 5.0)
                tone = sum(math.sin(2 * math.pi * f * t) for f in chord_freqs) / len(chord_freqs)
                samples[idx] += tone * decay * amp * 0.8
    return samples

out_dir = "app/src/main/res/raw"
os.makedirs(out_dir, exist_ok=True)

write_wav(os.path.join(out_dir, "dice_roll.wav"), gen_dice_roll())
write_wav(os.path.join(out_dir, "token_step.wav"), gen_token_step())
write_wav(os.path.join(out_dir, "roll_six.wav"), gen_roll_six())
write_wav(os.path.join(out_dir, "token_capture.wav"), gen_token_capture())
write_wav(os.path.join(out_dir, "victory.wav"), gen_victory())

print("Audio files generated successfully in", out_dir)
