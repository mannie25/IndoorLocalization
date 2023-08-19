import numpy as np
import matplotlib.pyplot as plt

actual_dist = 3.1
steps_taken = 5

print('\nErrors in each step length calculated')
step_lenx1 = [
    3.5908/4, 4.0951/5, 2.6814/3, 4.1749/5, 3.2775/4,
    3.8699/4, 3.7646/5, 1.788/2, 3.5782/4, 2.3845/3,
    3.0522/4, 2.039/3, 2.1463/3, 2.4269/3, 2.2097/3,
    3.7040/5, 1.6342/2, 1.6628/2, 2.4924/3, 3.7306/5
]

step_lenx1_errors = np.array(step_lenx1) - actual_dist / steps_taken

print('Mean:', np.mean(step_lenx1_errors))
print('Std:', np.std(step_lenx1_errors))

error_counts, error_bins = np.histogram(step_lenx1_errors, bins=6)
pdf = error_counts / len(step_lenx1_errors)
cdf = np.cumsum(pdf)

plt.plot(error_bins[:-1], cdf, alpha=0.8)
plt.fill_between(error_bins[:-1], pdf, alpha=0.2)
plt.show()

print('\nError in step length combined with step detection')
step_lenx5 = [
    3.5908, 4.0951, 2.6814, 4.1749, 3.2775,
    3.8699, 3.7646, 1.788, 3.5782, 2.3845,
    3.0522, 2.039, 2.1463, 2.4269, 2.2097,
    3.7040, 1.6342, 1.6628, 2.4924, 3.7306
]

step_lenx5_errors = np.array(step_lenx5) - actual_dist
step_len_errors = np.array(step_lenx5_errors) / steps_taken

print('Mean:', np.mean(step_len_errors))
print('Std:', np.std(step_len_errors))

error_counts, error_bins = np.histogram(step_len_errors, bins=6)
pdf = error_counts / len(step_len_errors)
cdf = np.cumsum(pdf)

plt.plot(error_bins[:-1], cdf, alpha=0.8)
plt.fill_between(error_bins[:-1], pdf, alpha=0.2)
plt.show()
