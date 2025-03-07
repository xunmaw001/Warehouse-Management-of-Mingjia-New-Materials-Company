
package com.controller;

import java.io.File;
import java.math.BigDecimal;
import java.net.URL;
import java.text.SimpleDateFormat;
import com.alibaba.fastjson.JSONObject;
import java.util.*;
import org.springframework.beans.BeanUtils;
import javax.servlet.http.HttpServletRequest;
import org.springframework.web.context.ContextLoader;
import javax.servlet.ServletContext;
import com.service.TokenService;
import com.utils.*;
import java.lang.reflect.InvocationTargetException;

import com.service.DictionaryService;
import org.apache.commons.lang3.StringUtils;
import com.annotation.IgnoreAuth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.baomidou.mybatisplus.mapper.Wrapper;
import com.entity.*;
import com.entity.view.*;
import com.service.*;
import com.utils.PageUtils;
import com.utils.R;
import com.alibaba.fastjson.*;

/**
 * 材料
 * 后端接口
 * @author
 * @email
*/
@RestController
@Controller
@RequestMapping("/cailiao")
public class CailiaoController {
    private static final Logger logger = LoggerFactory.getLogger(CailiaoController.class);

    @Autowired
    private CailiaoService cailiaoService;


    @Autowired
    private TokenService tokenService;
    @Autowired
    private DictionaryService dictionaryService;

    //级联表service
    @Autowired
    private CangkuService cangkuService;
    @Autowired
    private YonghuService yonghuService;



    /**
    * 后端列表
    */
    @RequestMapping("/page")
    public R page(@RequestParam Map<String, Object> params, HttpServletRequest request){
        logger.debug("page方法:,,Controller:{},,params:{}",this.getClass().getName(),JSONObject.toJSONString(params));
        String role = String.valueOf(request.getSession().getAttribute("role"));
        if(false)
            return R.error(511,"永不会进入");
        else if("用户".equals(role))
            params.put("yonghuId",request.getSession().getAttribute("userId"));
        params.put("cailiaoDeleteStart",1);params.put("cailiaoDeleteEnd",1);
        if(params.get("orderBy")==null || params.get("orderBy")==""){
            params.put("orderBy","id");
        }
        PageUtils page = cailiaoService.queryPage(params);

        //字典表数据转换
        List<CailiaoView> list =(List<CailiaoView>)page.getList();
        for(CailiaoView c:list){
            //修改对应字典表字段
            dictionaryService.dictionaryConvert(c, request);
        }
        return R.ok().put("data", page);
    }

    /**
    * 后端详情
    */
    @RequestMapping("/info/{id}")
    public R info(@PathVariable("id") Long id, HttpServletRequest request){
        logger.debug("info方法:,,Controller:{},,id:{}",this.getClass().getName(),id);
        CailiaoEntity cailiao = cailiaoService.selectById(id);
        if(cailiao !=null){
            //entity转view
            CailiaoView view = new CailiaoView();
            BeanUtils.copyProperties( cailiao , view );//把实体数据重构到view中

                //级联表
                CangkuEntity cangku = cangkuService.selectById(cailiao.getCangkuId());
                if(cangku != null){
                    BeanUtils.copyProperties( cangku , view ,new String[]{ "id", "createTime", "insertTime", "updateTime", "yonghuId"});//把级联的数据添加到view中,并排除id和创建时间字段
                    view.setCangkuId(cangku.getId());
                    view.setCangkuYonghuId(cangku.getYonghuId());
                }
                //级联表
                YonghuEntity yonghu = yonghuService.selectById(cailiao.getYonghuId());
                if(yonghu != null){
                    BeanUtils.copyProperties( yonghu , view ,new String[]{ "id", "createTime", "insertTime", "updateTime"});//把级联的数据添加到view中,并排除id和创建时间字段
                    view.setYonghuId(yonghu.getId());
                }
            //修改对应字典表字段
            dictionaryService.dictionaryConvert(view, request);
            return R.ok().put("data", view);
        }else {
            return R.error(511,"查不到数据");
        }

    }

    /**
    * 后端保存
    */
    @RequestMapping("/save")
    public R save(@RequestBody CailiaoEntity cailiao, HttpServletRequest request){
        logger.debug("save方法:,,Controller:{},,cailiao:{}",this.getClass().getName(),cailiao.toString());

        String role = String.valueOf(request.getSession().getAttribute("role"));
        if(false)
            return R.error(511,"永远不会进入");
        else if("用户".equals(role))
            cailiao.setYonghuId(Integer.valueOf(String.valueOf(request.getSession().getAttribute("userId"))));

        Wrapper<CailiaoEntity> queryWrapper = new EntityWrapper<CailiaoEntity>()
            .eq("yonghu_id", cailiao.getYonghuId())
            .eq("cangku_id", cailiao.getCangkuId())
            .eq("cailiao_uuid_number", cailiao.getCailiaoUuidNumber())
            .eq("cailiao_name", cailiao.getCailiaoName())
            .eq("cailiao_types", cailiao.getCailiaoTypes())
            .eq("cailiao_number", cailiao.getCailiaoNumber())
            .eq("cailiao_delete", cailiao.getCailiaoDelete())
            ;

        logger.info("sql语句:"+queryWrapper.getSqlSegment());
        CailiaoEntity cailiaoEntity = cailiaoService.selectOne(queryWrapper);
        if(cailiaoEntity==null){
            cailiao.setCailiaoDelete(1);
            cailiao.setInsertTime(new Date());
            cailiao.setCreateTime(new Date());
            cailiaoService.insert(cailiao);
            return R.ok();
        }else {
            return R.error(511,"表中有相同数据");
        }
    }

    /**
    * 后端修改
    */
    @RequestMapping("/update")
    public R update(@RequestBody CailiaoEntity cailiao, HttpServletRequest request){
        logger.debug("update方法:,,Controller:{},,cailiao:{}",this.getClass().getName(),cailiao.toString());

        String role = String.valueOf(request.getSession().getAttribute("role"));
//        if(false)
//            return R.error(511,"永远不会进入");
//        else if("用户".equals(role))
//            cailiao.setYonghuId(Integer.valueOf(String.valueOf(request.getSession().getAttribute("userId"))));
        //根据字段查询是否有相同数据
        Wrapper<CailiaoEntity> queryWrapper = new EntityWrapper<CailiaoEntity>()
            .notIn("id",cailiao.getId())
            .andNew()
            .eq("yonghu_id", cailiao.getYonghuId())
            .eq("cangku_id", cailiao.getCangkuId())
            .eq("cailiao_uuid_number", cailiao.getCailiaoUuidNumber())
            .eq("cailiao_name", cailiao.getCailiaoName())
            .eq("cailiao_types", cailiao.getCailiaoTypes())
            .eq("cailiao_number", cailiao.getCailiaoNumber())
            .eq("cailiao_delete", cailiao.getCailiaoDelete())
            ;

        logger.info("sql语句:"+queryWrapper.getSqlSegment());
        CailiaoEntity cailiaoEntity = cailiaoService.selectOne(queryWrapper);
        if("".equals(cailiao.getCailiaoPhoto()) || "null".equals(cailiao.getCailiaoPhoto())){
                cailiao.setCailiaoPhoto(null);
        }
        cailiao.setUpdateTime(new Date());
        if(cailiaoEntity==null){
            cailiaoService.updateById(cailiao);//根据id更新
            return R.ok();
        }else {
            return R.error(511,"表中有相同数据");
        }
    }

    /**
    * 删除
    */
    @RequestMapping("/delete")
    public R delete(@RequestBody Integer[] ids){
        logger.debug("delete:,,Controller:{},,ids:{}",this.getClass().getName(),ids.toString());
        ArrayList<CailiaoEntity> list = new ArrayList<>();
        for(Integer id:ids){
            CailiaoEntity cailiaoEntity = new CailiaoEntity();
            cailiaoEntity.setId(id);
            cailiaoEntity.setCailiaoDelete(2);
            list.add(cailiaoEntity);
        }
        if(list != null && list.size() >0){
            cailiaoService.updateBatchById(list);
        }
        return R.ok();
    }


    /**
     * 批量上传
     */
    @RequestMapping("/batchInsert")
    public R save( String fileName, HttpServletRequest request){
        logger.debug("batchInsert方法:,,Controller:{},,fileName:{}",this.getClass().getName(),fileName);
        Integer yonghuId = Integer.valueOf(String.valueOf(request.getSession().getAttribute("userId")));
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
            List<CailiaoEntity> cailiaoList = new ArrayList<>();//上传的东西
            Map<String, List<String>> seachFields= new HashMap<>();//要查询的字段
            Date date = new Date();
            int lastIndexOf = fileName.lastIndexOf(".");
            if(lastIndexOf == -1){
                return R.error(511,"该文件没有后缀");
            }else{
                String suffix = fileName.substring(lastIndexOf);
                if(!".xls".equals(suffix)){
                    return R.error(511,"只支持后缀为xls的excel文件");
                }else{
                    URL resource = this.getClass().getClassLoader().getResource("../../upload/" + fileName);//获取文件路径
                    File file = new File(resource.getFile());
                    if(!file.exists()){
                        return R.error(511,"找不到上传文件，请联系管理员");
                    }else{
                        List<List<String>> dataList = PoiUtil.poiImport(file.getPath());//读取xls文件
                        dataList.remove(0);//删除第一行，因为第一行是提示
                        for(List<String> data:dataList){
                            //循环
                            CailiaoEntity cailiaoEntity = new CailiaoEntity();
//                            cailiaoEntity.setYonghuId(Integer.valueOf(data.get(0)));   //用户 要改的
//                            cailiaoEntity.setCangkuId(Integer.valueOf(data.get(0)));   //仓库 要改的
//                            cailiaoEntity.setCailiaoUuidNumber(data.get(0));                    //材料编号 要改的
//                            cailiaoEntity.setCailiaoName(data.get(0));                    //材料名称 要改的
//                            cailiaoEntity.setCailiaoPhoto("");//详情和图片
//                            cailiaoEntity.setCailiaoTypes(Integer.valueOf(data.get(0)));   //材料类型 要改的
//                            cailiaoEntity.setCailiaoNumber(Integer.valueOf(data.get(0)));   //材料数量 要改的
//                            cailiaoEntity.setCailiaoContent("");//详情和图片
//                            cailiaoEntity.setCailiaoDelete(1);//逻辑删除字段
//                            cailiaoEntity.setInsertTime(date);//时间
//                            cailiaoEntity.setUpdateTime(sdf.parse(data.get(0)));          //最后更新时间 要改的
//                            cailiaoEntity.setCreateTime(date);//时间
                            cailiaoList.add(cailiaoEntity);


                            //把要查询是否重复的字段放入map中
                                //材料编号
                                if(seachFields.containsKey("cailiaoUuidNumber")){
                                    List<String> cailiaoUuidNumber = seachFields.get("cailiaoUuidNumber");
                                    cailiaoUuidNumber.add(data.get(0));//要改的
                                }else{
                                    List<String> cailiaoUuidNumber = new ArrayList<>();
                                    cailiaoUuidNumber.add(data.get(0));//要改的
                                    seachFields.put("cailiaoUuidNumber",cailiaoUuidNumber);
                                }
                        }

                        //查询是否重复
                         //材料编号
                        List<CailiaoEntity> cailiaoEntities_cailiaoUuidNumber = cailiaoService.selectList(new EntityWrapper<CailiaoEntity>().in("cailiao_uuid_number", seachFields.get("cailiaoUuidNumber")).eq("cailiao_delete", 1));
                        if(cailiaoEntities_cailiaoUuidNumber.size() >0 ){
                            ArrayList<String> repeatFields = new ArrayList<>();
                            for(CailiaoEntity s:cailiaoEntities_cailiaoUuidNumber){
                                repeatFields.add(s.getCailiaoUuidNumber());
                            }
                            return R.error(511,"数据库的该表中的 [材料编号] 字段已经存在 存在数据为:"+repeatFields.toString());
                        }
                        cailiaoService.insertBatch(cailiaoList);
                        return R.ok();
                    }
                }
            }
        }catch (Exception e){
            e.printStackTrace();
            return R.error(511,"批量插入数据异常，请联系管理员");
        }
    }






}
