import numpy as np
from simulate import sim_lin, sim_circle
from math import sin, cos


def analyse_line(results):
    errors = []
    for i, pos in zip(range(0, 15+1), results):
        errors.append((pos[0], pos[1] - i))
    errors = np.array(errors)
    print('Mean Error:', np.mean(errors))
    print('Max Error:', np.max(errors))


def analyse_circle(results):
    errors = []
    for deg, pos in zip(range(0, 360+1, 20), results):
        rad = ((180 - deg) * 3.1415) / 180
        x_c = 5 * cos(rad)
        y_c = 5 * sin(rad)
        errors.append((pos[0] - x_c, pos[1] - y_c))
    errors = np.array(errors)
    print('Mean:', np.mean(errors))
    print('Max:', np.max(errors))


if __name__ == '__main__':
    results = sim_lin()
    analyse_line(results)

    results = sim_circle()
    analyse_circle(results)
