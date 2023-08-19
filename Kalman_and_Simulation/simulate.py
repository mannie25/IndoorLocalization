from random import normalvariate
import matplotlib.pyplot as plt
from math import sin, cos, sqrt
from kalman import pdr, pdr_with_rtt, kalman_init_line, kalman_init_circle


def sim_lin():
    plt.style.use("seaborn-darkgrid")
    results = []

    print("\nLine Simulation")
    bel_pos, bel_cov = kalman_init_line()
    print(bel_pos, bel_cov)
    results.append((0, 0))
    plt.scatter(bel_pos[0], bel_pos[1], c='r', alpha=0.5)
    for i in range(0, 15+1)[1:]:
        step_len = 1 + normalvariate(-0.03697, 0.167)
        heading = 90 + normalvariate(-90*0.0661*(1+i/15), 0.0158*90)
        if i % 3 != 0:
            bel_pos, bel_cov = pdr(step_len, heading)
        else:
            bel_pos, bel_cov = pdr_with_rtt(step_len, heading)
        print(bel_pos, bel_cov)
        results.append(bel_pos)
        plt.scatter(bel_pos[0], bel_pos[1], c='b', alpha=0.5)
    plt.ylim(-1, 16)
    plt.xlim(-5, 5)
    plt.show()

    return results


def sim_circle():
    plt.style.use("seaborn-darkgrid")
    results = []

    print("\nCircle Simulation")
    bel_pos, bel_cov = kalman_init_circle()
    print(bel_pos, bel_cov)
    plt.scatter(bel_pos[0], bel_pos[1], c='r', alpha=0.5)
    deg_turned_per_step = 20
    radius = 5
    for deg in range(0, 360+1, deg_turned_per_step)[1:]:
        step_len = 1.736 + normalvariate(-0.03697, 0.167)
        heading = 90 - deg + normalvariate(-0.0661*(1+deg/36), 0.0158)
        if deg % 60 != 0:
            bel_pos, bel_cov = pdr(step_len, heading)
        else:
            bel_pos, bel_cov = pdr_with_rtt(step_len, heading)
        print(bel_pos, bel_cov)
        results.append(bel_pos)
        plt.scatter(bel_pos[0], bel_pos[1], c='b', alpha=0.5)
    plt.show()

    return results


if __name__ == '__main__':
    sim_lin()
    sim_circle()
