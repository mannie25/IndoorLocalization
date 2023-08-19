from math import sqrt, cos, sin
from random import normalvariate

x_a = 5
y_a = 0

l_ranges = []


def gen_rtt_line(len, step):
    for y_l in range(0, len+1, step)[1:]:
        rho = sqrt((x_a)**2 + (y_l - y_a)**2)
        # 1 meter accuracy 95% of the time
        rho = rho + normalvariate(0, 0.5)
        l_ranges.append(rho)
    # print(l_ranges)


def gen_rtt_circle(radius, step_deg):
    global l_ranges
    l_ranges = []
    for deg in range(0, 360+1, step_deg)[1:]:
        rad = ((180 - deg) * 3.1415) / 180
        x_c = radius * cos(rad)
        y_c = radius * sin(rad)
        rho = sqrt((x_c - x_a)**2 + (y_c - y_a)**2)
        # 1 meter accuracy 95% of the time
        rho = rho + normalvariate(0, 0.5)
        l_ranges.append(round(rho, 3))
    # print(l_ranges)


def read_rtt(loc):
    x_p, y_p = loc
    dist = sqrt((x_p - x_a)**2 + (y_p - y_a)**2)
    rho = l_ranges.pop(0)
    sf = rho/dist
    x_c = x_a + (x_p - x_a)*sf
    y_c = y_a + (y_p - y_a)*sf
    # print((x_p, y_p), (x_c, y_c))
    return x_c, y_c


# gen_rtt_line(15, 3)
# while True:
#     print(read_rtt())
