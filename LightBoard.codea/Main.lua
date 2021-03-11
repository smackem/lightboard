drawing = {
    figures = {}
}
currentFigure = nil
currentTool = defaultTool

function setup()
    -- Setting this tells the renderer to retain the previous frame when drawing
    -- you should use this if you do not call background in draw.
    -- Set it to `false` to see the difference (`false` is the default)
    viewer.retainedBacking = false
    
    print("Touch and drag on the screen")
    
    parameter.color("strokeColor", color(0))
    parameter.integer("strokeThickness", 1, 12, 2)
    parameter.boolean("erase", false, onEraseToggled)
    parameter.action("new", function() requestDrawing("POST", "/drawing/new") end)
    parameter.action("back", function() requestDrawing("POST", "/drawing/prev") end)
    parameter.action("forward", function() requestDrawing("POST", "/drawing/next") end)
    parameter.text("host", "192.168.178.48", function(v)
        print(v)
        sendInit(v)
    end)
    
    osc.port = 7771
    sendInit(host)
end

function sendInit(hostName)
    osc.host = hostName
    osc.send("/init/size", WIDTH, HEIGHT)
end

function draw()
    smooth()
    
    fill(255)
    background(255)
    for i,figure in ipairs(drawing.figures) do
        drawFigure(figure)
    end
    if currentFigure ~= nil then
        drawFigure(currentFigure)
    end
end

function drawFigure(figure)
    sc = figure.color
    stroke(sc.r, sc.g, sc.b, sc.a)
    strokeWidth(figure.strokeWidth)
    
    prevPt = nil
    for i,pt in ipairs(figure.points) do
        if prevPt ~= nil then
            line(prevPt.x, prevPt.y, pt.x, pt.y)
        end
        prevPt = pt
    end
end

function onEraseToggled(value)
    if value then
        currentTool = eraseTool
    else
        currentTool = defaultTool
    end
    currentTool:init()
end

function torgba(color)
    return {
        r = math.tointeger(color.r),
        g = math.tointeger(color.g),
        b = math.tointeger(color.b),
        a = math.tointeger(color.a)
    }
end

function requestDrawing(method, path)
    print("http request...")
    http.request("http://"..host..":7772"..path, function(body, status, headers)
        d = json.decode(body)
        print("http response "..status)
        --dump(d)
        for i,figure in ipairs(d.figures) do
            for j,pt in ipairs(figure.points) do
                pt.y = HEIGHT - pt.y
            end
        end
        drawing = d
    end, {
        method = method,
    })
end

function dump(t,indent)
    local names = {}
    if not indent then indent = "" end
    for n,g in pairs(t) do
        table.insert(names,n)
    end
    table.sort(names)
    for i,n in pairs(names) do
        local v = t[n]
        if type(v) == "table" then
            if(v==t) then -- prevent endless loop if table contains reference to itself
                print(indent..tostring(n)..": <-")
            else
                print(indent..tostring(n)..":")
                dump(v,indent.."   ")
            end
        else
            if type(v) == "function" then
                print(indent..tostring(n).."()")
            else
                print(indent..tostring(n)..": "..tostring(v))
            end
        end
    end
end

function touched(touch)
    if touch.state == BEGAN then
        currentTool:touchBegin(touch)
    elseif touch.state == MOVING then
        currentTool:touchMove(touch)
    elseif touch.state == ENDED then
        currentTool:touchEnd(touch)
    end
end

defaultTool = {}
function defaultTool:init()
end

function defaultTool:touchBegin(touch)
    sc = torgba(strokeColor)
    st = strokeThickness + 0.0
    osc.send("/figure/begin", touch.pos.x, HEIGHT - touch.pos.y, sc.r, sc.g, sc.b, sc.a, st)
    currentFigure = {
        color = sc,
        strokeWidth = st,
        points = {
            {x = touch.pos.x, y = touch.pos.y}
        }
    }
end

function defaultTool:touchMove(touch)
    osc.send("/figure/point", touch.pos.x, HEIGHT - touch.pos.y)
    table.insert(currentFigure.points, {x = touch.pos.x, y = touch.pos.y})
end

function defaultTool:touchEnd(touch)
    osc.send("/figure/end", touch.pos.x, HEIGHT - touch.pos.y)
    table.insert(drawing.figures, currentFigure)
    currentFigure = nil
    requestDrawing("GET", "/drawing")
end

eraseTool = {}
function eraseTool:init()
end

function eraseTool:touchBegin(touch)
end

function eraseTool:touchMove(touch)
    indices = {}
    for i,figure in ipairs(drawing.figures) do
        if isCloseToFigure(touch.pos.x, touch.pos.y, figure) then
            print("erase "..touch.pos.x.." "..touch.pos.y)
            table.insert(indices, i)
        end
    end
    for i = #indices, 1, -1 do
        table.remove(drawing.figures, indices[i])
        osc.send("/figure/remove", touch.pos.x, touch.pos.y, indices[i] - 1)
    end
end

function eraseTool:touchEnd(touch)
end

function isCloseToFigure(x, y, figure)
    prevPt = nil
    for i,pt in ipairs(figure.points) do
        if prevPt ~= nil then
            if isCloseToLine(x, y, prevPt, pt) then
                return true
            end
        end
        prevPt = pt
    end
    return false
end

function isCloseToLine(x, y, pt1, pt2)
    v1 = vec2(pt1.x, pt1.y)
    v2 = vec2(pt2.x, pt2.y)
    v = vec2(x, y)
    return math.abs(v:dist(v1) + v:dist(v2) - v1:dist(v2)) <= 10
end







