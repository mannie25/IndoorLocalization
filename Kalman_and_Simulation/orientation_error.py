import matplotlib.pyplot as plt
import numpy as np

heading = [
    81, 81, 84, 83, 85,
    84, 85, 86, 85, 86,
    86, 85, 84, 85, 84,
    83, 84, 84, 84, 82
]

actual_orientation = 90


orientation_errors = np.array(heading) - actual_orientation

orientation_errors_per_deg = orientation_errors / actual_orientation

print('Mean:', np.mean(orientation_errors_per_deg))
print('Std:', np.std(orientation_errors_per_deg))

error_counts, error_bins = np.histogram(orientation_errors_per_deg, bins=6)
pdf = error_counts / len(orientation_errors_per_deg)
cdf = np.cumsum(pdf)

plt.plot(error_bins[:-1], cdf, alpha=0.8)
plt.fill_between(error_bins[:-1], pdf, alpha=0.2)
plt.show()
